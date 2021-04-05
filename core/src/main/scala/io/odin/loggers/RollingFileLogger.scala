package io.odin.loggers

import java.nio.file.{Files, OpenOption, Path, Paths}
import java.time.{Instant, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import cats.Monad
import cats.effect.kernel.{Async, Clock, Fiber, Ref, Resource}
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{Level, Logger, LoggerMessage}

import scala.concurrent.duration.{FiniteDuration, _}

object RollingFileLogger {

  def apply[F[_]](
      fileNamePattern: LocalDateTime => String,
      maxFileSizeInBytes: Option[Long],
      rolloverInterval: Option[FiniteDuration],
      formatter: Formatter,
      minLevel: Level,
      openOptions: Seq[OpenOption] = Seq.empty
  )(implicit F: Async[F]): Resource[F, Logger[F]] = {
    new RollingFileLoggerFactory(
      fileNamePattern,
      maxFileSizeInBytes,
      rolloverInterval,
      formatter,
      minLevel,
      FileLogger.apply[F],
      openOptions = openOptions
    ).mk
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

    val df: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

    def mk: Resource[F, Logger[F]] = {
      val logger = for {
        ((logger, watcherFiber), release) <- allocate.allocated
        refLogger <- Ref.of(logger)
        refRelease <- Ref.of(release)
        _ <- F.start(rollingLoop(watcherFiber, refLogger, refRelease))
      } yield {
        (RefLogger(refLogger, minLevel), refRelease)
      }
      Resource.make(logger)(_._2.get.flatten).map {
        case (logger, _) => logger
      }
    }

    def now: F[Long] = F.realTime.map(_.toMillis)

    /**
      * Create file logger along with the file watcher
      */
    def allocate: Resource[F, (Logger[F], Fiber[F, Throwable, Unit])] =
      Resource.suspend(now.map { time =>
        val localTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), TimeZone.getDefault.toZoneId)
        val fileName = fileNamePattern(localTime)
        underlyingLogger(fileName, formatter, minLevel, openOptions).product(fileWatcher(fileName))
      })

    /**
      * Create resource with fiber that's cancelled on resource release.
      *
      * Fiber itself is a file watcher that checks if rollover interval or size are not exceeded and finishes it work
      * the moment at least one of those conditions is met.
      */
    def fileWatcher(fileName: String): Resource[F, Fiber[F, Throwable, Unit]] = {
      def checkConditions(start: Long, now: Long, fileSize: Long): Boolean = {
        (maxFileSizeInBytes match {
          case Some(max) => fileSize >= max
          case _         => false
        }) || (rolloverInterval match {
          case Some(finite: FiniteDuration) =>
            start + finite.toMillis <= now
          case _ => false
        })
      }

      def loop(start: Long): F[Unit] = {
        for {
          size <- if (maxFileSizeInBytes.isDefined) {
            F.delay(fileSizeCheck(Paths.get(fileName)))
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

      Resource.make[F, Fiber[F, Throwable, Unit]](F.start(now >>= loop))(_.cancel)
    }

    /**
      * Once watcher fiber is joined, it means that it's triggered and current logger's file exceeded TTL or allowed size.
      * At this moment new logger, new watcher and new release values shall be allocated to replace the old ones.
      *
      * Once new values are allocated and corresponding references are updated, run the old release and loop the whole
      * function using new watcher
      */
    def rollingLoop(watcher: Fiber[F, Throwable, Unit], logger: Ref[F, Logger[F]], release: Ref[F, F[Unit]]): F[Unit] =
      for {
        _ <- watcher.join
        oldRelease <- release.get
        ((newLogger, newWatcher), newRelease) <- allocate.allocated
        _ <- logger.set(newLogger)
        _ <- release.set(newRelease)
        _ <- oldRelease
        _ <- rollingLoop(newWatcher, logger, release)
      } yield ()

  }

}
