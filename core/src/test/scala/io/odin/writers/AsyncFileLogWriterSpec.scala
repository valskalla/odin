package io.odin.writers

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.instances.list._
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}

import scala.concurrent.duration._

class AsyncFileLogWriterSpec extends OdinSpec {

  private val fileResource = Resource.make[IO, Path] {
    IO.delay(Files.createFile(Paths.get(UUID.randomUUID().toString)))
  } { file =>
    IO.delay(Files.delete(file))
  }

  private val ec = scala.concurrent.ExecutionContext.global
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  it should "write formatted messages into file" in {
    forAll { loggerMessage: List[LoggerMessage] =>
      fileResource
        .flatMap { path =>
          val fileName = path.toString
          Resource
            .liftF {
              for {
                writer <- AsyncFileLogWriter[IO](fileName, 5.millis)
                _ <- loggerMessage.traverse(writer.write(_, Formatter.simple))
                _ <- timer.sleep(10.millis)
              } yield {
                new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
                  .map(Formatter.simple.format)
                  .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
              }
            }
        }
        .use(IO(_))
        .unsafeRunSync()
    }
  }

  it should "not flush on its own" in {
    forAll { loggerMessage: LoggerMessage =>
      fileResource
        .flatMap { path =>
          val fileName = path.toString
          val writer = new AsyncFileLogWriter[IO](Files.newBufferedWriter(Paths.get(fileName)), 5.millis)
          Resource
            .liftF {
              for {
                _ <- writer.write(loggerMessage, Formatter.simple)
                _ <- timer.sleep(10.millis)
              } yield {
                new String(Files.readAllBytes(Paths.get(fileName))) shouldBe empty
              }
            }
        }
        .use(IO(_))
        .unsafeRunSync()
    }
  }

}
