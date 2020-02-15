package io.odin.loggers

import java.nio.file.{Files, Path}
import java.util.UUID

import cats.effect.Resource
import io.odin.formatter.Formatter
import io.odin.util.ListDirectory
import io.odin.{asyncRollingFileLogger, Level, LoggerMessage, OdinSpec}
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

import scala.concurrent.duration._

class RollingFileLoggerSpec extends OdinSpec {

  implicit private val scheduler: TestScheduler = TestScheduler()

  private val fileResource = Resource.make[Task, Path] {
    Task.delay(Files.createTempDirectory(UUID.randomUUID().toString))
  } { file =>
    Task.delay {
      ListDirectory(file).filter(_.isFile).foreach(_.delete())
      Files.delete(file)
    }
  }

  it should "write formatted message into file" in {
    forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
      (for {
        path <- fileResource
        filePrefix = path.toString + "/"
        logger <- RollingFileLogger[Task](
          filePrefix,
          maxFileSizeInBytes = None,
          rolloverInterval = None,
          formatter = formatter,
          minLevel = Level.Trace
        )
        _ <- Resource.liftF(logger.log(loggerMessage))
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
        filePrefix = path.toString + "/"
        logger <- RollingFileLogger[Task](
          filePrefix,
          maxFileSizeInBytes = None,
          rolloverInterval = None,
          formatter = formatter,
          minLevel = Level.Trace
        )
        _ <- Resource.liftF(logger.log(loggerMessage))
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
        filePrefix = path.toString + "/"
        logger <- asyncRollingFileLogger[Task](
          filePrefix,
          rolloverInterval = None,
          maxFileSizeInBytes = None,
          formatter = formatter,
          minLevel = Level.Trace
        )
        _ <- Resource.liftF(logger.withMinimalLevel(Level.Trace).log(loggerMessage))
        _ = scheduler.tick(2.seconds)
      } yield {
        val logFile = ListDirectory(path).filter(_.isFile).head.toPath
        new String(Files.readAllBytes(logFile)) shouldBe loggerMessage
          .map(formatter.format)
          .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
      }).use(Task(_))
        .runSyncUnsafe()
    }
  }

  it should "append file prefix with date time" in {
    
  }

}
