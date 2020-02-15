package io.odin.loggers

import java.nio.file.{Files, Path, Paths}
import java.time.{Instant, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import java.util.concurrent.TimeUnit

import cats.Monad
import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift, Fiber, Resource, Timer}
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{Level, Logger, LoggerMessage}

import scala.concurrent.duration.{FiniteDuration, _}

object RollingFileLogger {

  def apply[F[_]](
      fileNamePrefix: String,
      maxFileSizeInBytes: Option[Long],
      rolloverInterval: Option[FiniteDuration],
      formatter: Formatter,
      minLevel: Level
  )(implicit F: Concurrent[F], timer: Timer[F], cs: ContextShift[F]): Resource[F, Logger[F]] = {
    new RollingFileLoggerFactory(
      fileNamePrefix,
      maxFileSizeInBytes,
      rolloverInterval,
      formatter,
      minLevel,
      FileLogger.apply[F]
    ).mk
  }

  private[odin] class RefLogger[F[_]: Timer: Monad](
      current: Ref[F, Logger[F]],
      override val minLevel: Level
  ) extends DefaultLogger[F](minLevel) {

    def log(msg: LoggerMessage): F[Unit] = current.get.flatMap(_.log(msg))

    override def log(msgs: List[LoggerMessage]): F[Unit] = current.get.flatMap(_.log(msgs))

  }

  private[odin] class RollingFileLoggerFactory[F[_]](
      fileNamePrefix: String,
      maxFileSizeInBytes: Option[Long],
      rolloverInterval: Option[FiniteDuration],
      formatter: Formatter,
      minLevel: Level,
      underlyingLogger: (String, Formatter, Level) => Resource[F, Logger[F]],
      fileSizeCheck: Path => Long = Files.size
  )(implicit F: Concurrent[F], timer: Timer[F], cs: ContextShift[F]) {

    val df: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

    def mk: Resource[F, Logger[F]] = {
      val logger = for {
        loggerAndFiber <- allocate.allocated
        ((logger, fiber), release) = loggerAndFiber
        refLogger <- Ref.of(logger)
        refRelease <- Ref.of(release)
        _ <- F.start(rollingLoop(fiber, refLogger, refRelease))
      } yield {
        (new RefLogger(refLogger, minLevel), refRelease)
      }
      Resource.make(logger)(_._2.get.flatten).map {
        case (logger, _) => logger
      }
    }

    def now: F[Long] = timer.clock.monotonic(TimeUnit.MILLISECONDS)

    /**
      * Create file logger along with the file watcher
      */
    def allocate: Resource[F, (Logger[F], Fiber[F, Unit])] =
      Resource.suspend(now.map { time =>
        val localTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), TimeZone.getDefault.toZoneId)
        val fileName =
          s"$fileNamePrefix-${df.format(localTime)}"
        underlyingLogger(fileName, formatter, minLevel).product(fileWatcher(fileName))
      })

    /**
      * Create resource with fiber that's cancelled on resource release.
      *
      * Fiber itself is a file watcher that checks if rollover interval or size are not exceeded and finishes it work
      * the moment at least one of those conditions is met.
      */
    def fileWatcher(fileName: String): Resource[F, Fiber[F, Unit]] = {
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
          size <- F.delay(fileSizeCheck(Paths.get(fileName)))
          time <- now
          _ <- F.unlessA(checkConditions(start, time, size)) {
            for {
              _ <- cs.shift
              _ <- timer.sleep(100.millis)
              _ <- loop(start)
            } yield ()
          }
        } yield ()
      }

      Resource.make[F, Fiber[F, Unit]](F.start(now >>= loop))(_.cancel)
    }

    /**
      * Once watcher fiber is joined, it means that it's triggered and current logger's file exceeded TTL or allowed size.
      * At this moment new logger, new watcher and new release values shall be allocated to replace the old ones.
      *
      * Once new values are allocated and corresponding references are updated, run the old release and loop the whole
      * function using new watcher
      */
    def rollingLoop(watcher: Fiber[F, Unit], logger: Ref[F, Logger[F]], release: Ref[F, F[Unit]]): F[Unit] =
      for {
        _ <- watcher.join
        oldRelease <- release.get
        newLoggerAndWatcher <- allocate.allocated
        ((newLogger, newWatcher), newRelease) = newLoggerAndWatcher
        _ <- logger.set(newLogger)
        _ <- release.set(newRelease)
        _ <- oldRelease
        _ <- rollingLoop(newWatcher, logger, release)
      } yield ()

  }

}
