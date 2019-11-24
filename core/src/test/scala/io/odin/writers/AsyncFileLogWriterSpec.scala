package io.odin.writers

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.instances.list._
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}
import org.scalatest.Assertion
import retry._
import retry.CatsEffect._

import scala.concurrent.duration._

class AsyncFileLogWriterSpec extends OdinSpec {

  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)
  private val retryPolicy = RetryPolicies.limitRetries[IO](5)

  private val fileResource = Resource.make[IO, Path] {
    IO.delay(Files.createTempFile(UUID.randomUUID().toString, ""))
  } { file =>
    IO.delay(Files.delete(file))
  }

  it should "write formatted messages into file" in {
    forAll { loggerMessage: List[LoggerMessage] =>
      retryingOnAllErrors[Assertion](policy = retryPolicy, onError = (_: Throwable, _) => IO.unit) {
        (for {
          path <- fileResource
          fileName = path.toString
          writer <- AsyncFileLogWriter[IO](fileName, 5.millis)
          _ <- Resource.liftF(loggerMessage.traverse(writer.write(_, Formatter.default)))
          _ <- Resource.liftF(timer.sleep(100.millis))
        } yield {
          new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
            .map(Formatter.default.format)
            .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
        }).use(IO(_))
      }.unsafeRunSync()
    }
  }

  it should "not flush on its own" in {
    forAll { loggerMessage: LoggerMessage =>
      retryingOnAllErrors[Assertion](policy = retryPolicy, onError = (_: Throwable, _) => IO.unit) {
        fileResource
          .flatMap { path =>
            val fileName = path.toString
            val writer = new AsyncFileLogWriter[IO](Files.newBufferedWriter(Paths.get(fileName)), 5.millis)
            Resource
              .liftF {
                for {
                  _ <- writer.write(loggerMessage, Formatter.default)
                  _ <- timer.sleep(200.millis)
                } yield {
                  new String(Files.readAllBytes(Paths.get(fileName))) shouldBe empty
                }
              }
          }
          .use(IO(_))
      }.unsafeRunSync()
    }
  }

}
