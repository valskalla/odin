package io.odin.loggers

import java.nio.file.{Files, Path}
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime}
import java.util.{TimeZone, UUID}
import java.util.concurrent.TimeUnit

import cats.effect.{Resource, Timer}
import io.odin.config._
import io.odin.formatter.Formatter
import io.odin.util.ListDirectory
import io.odin.{asyncRollingFileLogger, Level, LoggerMessage, OdinSpec}
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

import scala.concurrent.duration._

class RollingFileLoggerSpec extends OdinSpec {

  private val fileResource = Resource.make[Task, Path] {
    Task.delay(Files.createTempDirectory(UUID.randomUUID().toString))
  } { file =>
    Task.delay {
      ListDirectory(file).filter(_.isFile).foreach(_.delete())
      Files.delete(file)
    }
  }

  {
    implicit val scheduler: TestScheduler = TestScheduler()
    it should "write formatted message into file" in {
      forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
        (for {
          path <- fileResource
          filePrefix = path.toString
          logger <- RollingFileLogger[Task](
            file"$filePrefix/log-$year-$month-$day-$hour-$minute-$second.log",
            maxFileSizeInBytes = None,
            rolloverInterval = None,
            formatter = formatter,
            minLevel = Level.Trace
          )
          _ <- Resource.eval(logger.log(loggerMessage))
        } yield {
          val logFile = ListDirectory(path).filter(_.isFile).head.toPath
          new String(Files.readAllBytes(logFile)) shouldBe formatter.format(loggerMessage) + lineSeparator
        }).use(Task(_))
          .runSyncUnsafe()
      }
    }

    it should "write formatted messages into file" in {
      forAll { (loggerMessage: List[LoggerMessage], formatter: Formatter) =>
        (for {
          path <- fileResource
          filePrefix = path.toString
          logger <- RollingFileLogger[Task](
            file"$filePrefix/log-$year-$month-$day-$hour-$minute-$second.log",
            maxFileSizeInBytes = None,
            rolloverInterval = None,
            formatter = formatter,
            minLevel = Level.Trace
          )
          _ <- Resource.eval(logger.log(loggerMessage))
        } yield {
          val logFile = ListDirectory(path).filter(_.isFile).head.toPath
          new String(Files.readAllBytes(logFile)) shouldBe loggerMessage
            .map(formatter.format)
            .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
        }).use(Task(_))
          .runSyncUnsafe()
      }
    }

    it should "write in async mode" in {
      forAll { (loggerMessage: List[LoggerMessage], formatter: Formatter) =>
        (for {
          path <- fileResource
          filePrefix = path.toString
          logger <- asyncRollingFileLogger[Task](
            file"$filePrefix/log-$year-$month-$day-$hour-$minute-$second.log",
            rolloverInterval = None,
            maxFileSizeInBytes = None,
            formatter = formatter,
            minLevel = Level.Trace
          )
          _ <- Resource.eval(logger.withMinimalLevel(Level.Trace).log(loggerMessage))
          _ <- Resource.eval(Task(scheduler.tick(2.seconds)))
        } yield {
          val logFile = ListDirectory(path).filter(_.isFile).head.toPath
          new String(Files.readAllBytes(logFile)) shouldBe loggerMessage
            .map(formatter.format)
            .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
        }).use(Task(_))
          .runSyncUnsafe()
      }
    }

    it should "log file name should match the pattern" in {
      (for {
        path <- fileResource
        time <- Resource.eval(implicitly[Timer[Task]].clock.realTime(TimeUnit.MILLISECONDS))
        filePrefix = path.toString
        _ <- RollingFileLogger[Task](
          file"$filePrefix/log-$year-$month-$day-$hour-$minute-$second.log",
          maxFileSizeInBytes = None,
          rolloverInterval = None,
          formatter = Formatter.default,
          minLevel = Level.Trace
        )
      } yield {
        val logFile = ListDirectory(path).filter(_.isFile).head.toPath
        val localDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), TimeZone.getDefault.toZoneId)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
        val formatted = formatter.format(localDt)
        logFile.toString shouldBe s"$filePrefix/log-$formatted.log"
      }).use(Task(_))
        .runSyncUnsafe()
    }
  }

  {
    //@TODO TestScheduler is somewhat broken for this case
    import monix.execution.Scheduler.Implicits.global
    it should "write to the next file once interval is over" in {
      forAll { (lm1: LoggerMessage, lm2: LoggerMessage, formatter: Formatter) =>
        (for {
          path <- fileResource
          filePrefix = path.toString
          logger <- RollingFileLogger[Task](
            file"$filePrefix/log-$year-$month-$day-$hour-$minute-$second.log",
            maxFileSizeInBytes = None,
            rolloverInterval = Some(1.second),
            formatter = formatter,
            minLevel = Level.Trace
          )
          _ <- Resource.eval(logger.log(lm1))
          _ <- Resource.eval(Task.sleep(1200.millis))
          _ <- Resource.eval(logger.log(lm2))
        } yield {
          val log1 :: log2 :: Nil = ListDirectory(path).filter(_.isFile).sortBy(_.getName)
          new String(Files.readAllBytes(log1.toPath)) shouldBe formatter.format(lm1) + lineSeparator
          new String(Files.readAllBytes(log2.toPath)) shouldBe formatter.format(lm2) + lineSeparator
        }).use(Task(_))
          .runSyncUnsafe()
      }
    }

    it should "write to the next file once log file size is exceeded" in {
      forAll { (lm1: LoggerMessage, lm2: LoggerMessage, formatter: Formatter) =>
        (for {
          path <- fileResource
          filePrefix = path.toString
          logger <- RollingFileLogger[Task](
            file"$filePrefix/log-$year-$month-$day-$hour-$minute-$second.log",
            maxFileSizeInBytes = Some(1),
            rolloverInterval = None,
            formatter = formatter,
            minLevel = Level.Trace
          )
          _ <- Resource.eval(Task.sleep(1.second))
          _ <- Resource.eval(logger.log(lm1))
          _ <- Resource.eval(Task.sleep(1.second))
          _ <- Resource.eval(logger.log(lm2))
        } yield {
          val log1 :: log2 :: Nil = ListDirectory(path).filter(_.isFile).sortBy(_.getName)
          new String(Files.readAllBytes(log1.toPath)) shouldBe formatter.format(lm1) + lineSeparator
          new String(Files.readAllBytes(log2.toPath)) shouldBe formatter.format(lm2) + lineSeparator
        }).use(Task(_))
          .runSyncUnsafe()
      }
    }
  }

}
