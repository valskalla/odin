package io.odin.writers

import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import cats.effect.{IO, Resource}
import cats.instances.list._
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}
import org.scalatest.BeforeAndAfter

import scala.collection.mutable

class SyncFileLogWriterSpec extends OdinSpec with BeforeAndAfter {

  private val fileResource = Resource.make[IO, Path] {
    IO.delay(Files.createFile(Paths.get(UUID.randomUUID().toString)))
  } { file =>
    IO.delay(Files.delete(file))
  }

  it should "write formatted messages into file" in {
    forAll { loggerMessage: List[LoggerMessage] =>
      fileResource
        .flatMap { path =>
          val fileName = path.toString
          val writer = SyncFileLogWriter[IO](fileName)
          Resource
            .liftF(loggerMessage.traverse(writer.write(_, Formatter.simple)))
            .map { _ =>
              new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
                .map(Formatter.simple.format)
                .mkString(lineSeparator) + (if (loggerMessage.isEmpty) "" else lineSeparator)
            }
        }
        .use(IO(_))
        .unsafeRunSync()
    }
  }
}
