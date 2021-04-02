package io.odin.loggers

import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import cats.effect.{IO, Outcome, Resource}
import cats.effect.testkit.{TestContext, TestInstances}
import io.odin._
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}

import scala.concurrent.duration._

class FileLoggerSpec extends OdinSpec with TestInstances {

  private val fileResource = Resource.make[IO, Path] {
    IO.delay(Files.createTempFile(UUID.randomUUID().toString, ""))
  } { file =>
    IO.delay(Files.delete(file))
  }

  it should "write formatted message into file" in {
    forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
      import cats.effect.unsafe.implicits.global

      (for {
        path <- fileResource
        fileName = path.toString
        logger <- FileLogger[IO](fileName, formatter, Level.Trace)
        _ <- Resource.eval(logger.log(loggerMessage))
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe formatter.format(loggerMessage) + lineSeparator
      }).use(IO(_))
        .unsafeRunSync()
    }
  }

  it should "write formatted messages into file" in {
    forAll { (loggerMessage: List[LoggerMessage], formatter: Formatter) =>
      import cats.effect.unsafe.implicits.global

      (for {
        path <- fileResource
        fileName = path.toString
        logger <- FileLogger[IO](fileName, formatter, Level.Trace)
        _ <- Resource.eval(logger.log(loggerMessage))
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
          .map(formatter.format)
          .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
      }).use(IO(_))
        .unsafeRunSync()
    }
  }

  it should "write in async mode" in {
    implicit val ticker: Ticker = Ticker(TestContext())

    forAll { (loggerMessage: List[LoggerMessage], formatter: Formatter) =>
      val expected = loggerMessage
        .map(formatter.format)
        .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)

      val io = for {
        path <- fileResource
        fileName = path.toString
        logger <- asyncFileLogger[IO](fileName, formatter)
        _ <- Resource.eval(logger.withMinimalLevel(Level.Trace).log(loggerMessage))
        _ <- Resource.eval(IO(ticker.ctx.tick(2.seconds)))
      } yield new String(Files.readAllBytes(Paths.get(fileName)))

      unsafeRun(io.use(IO(_))) shouldBe Outcome.succeeded(Some(expected))
    }
  }
}
