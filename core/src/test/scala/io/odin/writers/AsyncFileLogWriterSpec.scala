package io.odin.writers

import java.nio.file.{Files, Paths}
import java.util.UUID

import cats.effect.{ContextShift, IO, Timer}
import io.odin.{LoggerMessage, OdinSpec}
import io.odin.formatter.Formatter

import scala.concurrent.duration._
import cats.syntax.all._
import cats.instances.list._
import org.scalatest.BeforeAndAfter

class AsyncFileLogWriterSpec extends OdinSpec with BeforeAndAfter {

  private val fileName = UUID.randomUUID().toString

  private val ec = scala.concurrent.ExecutionContext.global
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  it should "write formatted messages into file" in {
    forAll { loggerMessage: List[LoggerMessage] =>
      (for {
        writer <- AsyncFileLogWriter[IO](fileName, 5.millis)
        _ <- loggerMessage.traverse(writer.write(_, Formatter.simple))
        _ <- timer.sleep(10.millis)
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
          .map(Formatter.simple.format)
          .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
      }).unsafeRunSync()

    }
  }

  it should "not flush on its own" in {
    forAll { loggerMessage: LoggerMessage =>
      val logWriter = new AsyncFileLogWriter[IO](Files.newBufferedWriter(Paths.get(fileName)), 5.millis)
      (for {
        _ <- logWriter.write(loggerMessage, Formatter.simple)
        _ <- timer.sleep(10.millis)
      } yield {
        new String(Files.readAllBytes(Paths.get(fileName))) shouldBe empty
      }).unsafeRunSync()
    }
  }

  after {
    Files.delete(Paths.get(fileName))
  }

}
