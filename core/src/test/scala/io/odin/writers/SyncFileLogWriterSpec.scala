package io.odin.writers

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import cats.effect.{IO, Resource}
import cats.instances.list._
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}
import monix.eval.Task
import monix.execution.schedulers.TestScheduler
import org.scalatest.BeforeAndAfter

class SyncFileLogWriterSpec extends OdinSpec with BeforeAndAfter {

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
          val writer = SyncFileLogWriter[Task](fileName)
          Resource
            .liftF(loggerMessage.traverse(writer.write(_, Formatter.simple)))
            .map { _ =>
              new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
                .map(Formatter.simple.format)
                .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
            }
        }
        .use(Task(_))
        .runSyncUnsafe()
    }
  }
}
