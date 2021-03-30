package io.odin.loggers

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import cats.effect.Resource
import io.odin._
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}
import scala.concurrent.duration._
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

class FileLoggerSpec extends OdinSpec {
  implicit private val scheduler: TestScheduler = TestScheduler()

  private val fileResource = Resource.make[Task, Path] {
    Task.delay(Files.createTempFile(UUID.randomUUID().toString, ""))
  } { file =>
    Task.delay(Files.delete(file))
  }

  it should "write formatted message into file" in {
    forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
      (for {
        path <- fileResource
        fileName = path.toString
        logger <- FileLogger[Task](fileName, formatter, Level.Trace)
        _ <- Resource.eval(logger.log(loggerMessage))
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe formatter.format(loggerMessage) + lineSeparator
      }).use(Task(_))
        .runSyncUnsafe()
    }
  }

  it should "write formatted messages into file" in {
    forAll { (loggerMessage: List[LoggerMessage], formatter: Formatter) =>
      (for {
        path <- fileResource
        fileName = path.toString
        logger <- FileLogger[Task](fileName, formatter, Level.Trace)
        _ <- Resource.eval(logger.log(loggerMessage))
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
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
        fileName = path.toString
        logger <- asyncFileLogger[Task](fileName, formatter)
        _ <- Resource.eval(logger.withMinimalLevel(Level.Trace).log(loggerMessage))
        _ <- Resource.eval(Task(scheduler.tick(2.seconds)))
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
          .map(formatter.format)
          .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
      }).use(Task(_))
        .runSyncUnsafe()
    }
  }
}
