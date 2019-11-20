package io.odin.writers

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.instances.list._
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

import scala.concurrent.duration._

class AsyncFileLogWriterSpec extends OdinSpec {

  implicit private val scheduler: TestScheduler = TestScheduler()

  private val fileResource = Resource.make[Task, Path] {
    Task.delay(Files.createTempFile(UUID.randomUUID().toString, ""))
  } { file =>
    Task.delay(Files.delete(file))
  }

  it should "write formatted messages into file" in {
    forAll { loggerMessage: List[LoggerMessage] =>
      fileResource
        .flatMap { path =>
          val fileName = path.toString
          Resource
            .liftF {
              for {
                writer <- AsyncFileLogWriter[Task](fileName, 5.millis)
                _ <- loggerMessage.traverse(writer.write(_, Formatter.simple))
                _ = scheduler.tick(50.millis)
              } yield {
                new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
                  .map(Formatter.simple.format)
                  .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
              }
            }
        }
        .use(Task(_))
        .runSyncUnsafe()
    }
  }

  it should "not flush on its own" in {
    forAll { loggerMessage: LoggerMessage =>
      fileResource
        .flatMap { path =>
          val fileName = path.toString
          val writer = new AsyncFileLogWriter[Task](Files.newBufferedWriter(Paths.get(fileName)), 5.millis)
          Resource
            .liftF {
              for {
                _ <- writer.write(loggerMessage, Formatter.simple)
                _ = scheduler.tick(10.millis)
              } yield {
                new String(Files.readAllBytes(Paths.get(fileName))) shouldBe empty
              }
            }
        }
        .use(Task(_))
        .runSyncUnsafe()
    }
  }

}
