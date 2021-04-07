package io.odin.loggers

import java.nio.file.{Files, OpenOption, Path, Paths}
import java.time.LocalDateTime
import java.util.TimeZone
import cats.effect.kernel._
import cats.effect.std.Hotswap
import cats.syntax.all._
import cats.{Functor, Monad}
import io.odin.formatter.Formatter
import io.odin.{Level, Logger, LoggerMessage}

import scala.concurrent.duration._

object RollingFileLogger {

  def apply[F[_]](
      fileNamePattern: LocalDateTime => String,
      maxFileSizeInBytes: Option[Long],
      rolloverInterval: Option[FiniteDuration],
      formatter: Formatter,
      minLevel: Level,
      openOptions: Seq[OpenOption] = Seq.empty
  )(implicit F: Async[F]): Resource[F, Logger[F]] = {

    def rollingLogger =
      new RollingFileLoggerFactory(
        fileNamePattern,
        maxFileSizeInBytes,
        rolloverInterval,
        formatter,
        minLevel,
        FileLogger.apply[F],
        openOptions = openOptions
      ).mk

    def fileLogger =
      Resource.suspend {
        for {
          localTime <- localDateTimeNow
        } yield FileLogger[F](fileNamePattern(localTime), formatter, minLevel, openOptions)
      }

    Resource.pure[F, Boolean](maxFileSizeInBytes.isDefined || rolloverInterval.isDefined).ifM(rollingLogger, fileLogger)
  }

  private[odin] case class RefLogger[F[_]: Clock: Monad](
      current: Ref[F, Logger[F]],
      override val minLevel: Level
  ) extends DefaultLogger[F](minLevel) {

    def submit(msg: LoggerMessage): F[Unit] = current.get.flatMap(_.log(msg))

    override def submit(msgs: List[LoggerMessage]): F[Unit] = current.get.flatMap(_.log(msgs))

    def withMinimalLevel(level: Level): Logger[F] = copy(minLevel = level)
  }

  private[odin] class RollingFileLoggerFactory[F[_]](
      fileNamePattern: LocalDateTime => String,
      maxFileSizeInBytes: Option[Long],
      rolloverInterval: Option[FiniteDuration],
      formatter: Formatter,
      minLevel: Level,
      underlyingLogger: (String, Formatter, Level, Seq[OpenOption]) => Resource[F, Logger[F]],
      fileSizeCheck: Path => Long = Files.size,
      openOptions: Seq[OpenOption]
  )(implicit F: Async[F]) {

    private type RolloverSignal = Deferred[F, Unit]

    def mk: Resource[F, Logger[F]] =
      for {
        (hs, (logger, rolloverSignal)) <- Hotswap[F, (Logger[F], RolloverSignal)](allocate)
        refLogger <- Resource.eval(Ref.of(logger))
        _ <- F.background(rollingLoop(hs, rolloverSignal, refLogger))
      } yield RefLogger(refLogger, minLevel)

    private def now: F[Long] = F.realTime.map(_.toMillis)

    /**
      * Create file logger along with the file watcher
      */
    private def allocate: Resource[F, (Logger[F], RolloverSignal)] =
      Resource.suspend(localDateTimeNow.map { localTime =>
        val fileName = fileNamePattern(localTime)
        underlyingLogger(fileName, formatter, minLevel, openOptions).product(fileWatcher(Paths.get(fileName)))
      })

    /**
      * Create resource with fiber that's cancelled on resource release.
      *
      * Fiber itself is a file watcher that checks if rollover interval or size are not exceeded and finishes it work
      * the moment at least one of those conditions is met.
      */
    private def fileWatcher(filePath: Path): Resource[F, RolloverSignal] = {
      val checkFileSize: Long => Boolean =
        maxFileSizeInBytes match {
          case Some(max) => fileSize => fileSize >= max
          case _         => _ => false
        }

      val checkRolloverInterval: (Long, Long) => Boolean =
        rolloverInterval match {
          case Some(finite: FiniteDuration) =>
            (start, now) => start + finite.toMillis <= now
          case _ =>
            (_, _) => false
        }

      def checkConditions(start: Long, now: Long, fileSize: Long): Boolean =
        checkFileSize(fileSize) || checkRolloverInterval(start, now)

      def loop(start: Long): F[Unit] = {
        for {
          size <- if (maxFileSizeInBytes.isDefined) {
            F.delay(fileSizeCheck(filePath))
          } else {
            F.pure(0L)
          }
          time <- now
          _ <- F.unlessA(checkConditions(start, time, size)) {
            for {
              _ <- F.sleep(100.millis)
              _ <- loop(start)
            } yield ()
          }
        } yield ()
      }

      for {
        rolloverSignal <- Resource.eval(Deferred[F, Unit])
        _ <- F.background(now >>= loop >>= rolloverSignal.complete)
      } yield rolloverSignal
    }

    /**
      * Once rollover signal is sent, it means that it's triggered and current logger's file exceeded TTL or allowed size.
      * At this moment new logger, new watcher and new release values shall be allocated to replace the old ones.
      *
      * Once new values are allocated and corresponding references are updated, run the old release and loop the whole
      * function using new watcher
      */
    private def rollingLoop(
        hs: Hotswap[F, (Logger[F], RolloverSignal)],
        rolloverSignal: RolloverSignal,
        logger: Ref[F, Logger[F]]
    ): F[Unit] =
      F.tailRecM[RolloverSignal, Unit](rolloverSignal) { signal =>
        for {
          _ <- signal.get
          (newLogger, newSignal) <- hs.swap(allocate)
          _ <- logger.set(newLogger)
        } yield Left(newSignal)
      }

  }

  private def localDateTimeNow[F[_]: Functor](implicit clock: Clock[F]): F[LocalDateTime] =
    for {
      time <- clock.realTimeInstant
    } yield LocalDateTime.ofInstant(time, TimeZone.getDefault.toZoneId)

}
