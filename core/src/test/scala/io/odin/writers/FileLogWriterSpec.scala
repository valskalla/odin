package io.odin.writers

import java.nio.file.{Files, Paths}
import java.util.UUID

import cats.effect.IO
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}
import cats.syntax.all._
import cats.instances.list._
import org.scalatest.BeforeAndAfter

class FileLogWriterSpec extends OdinSpec with BeforeAndAfter {

  private val fileName = UUID.randomUUID().toString

  it should "write formatted messages into file" in {
    forAll { loggerMessage: List[LoggerMessage] =>
      val writer = FileLogWriter[IO](fileName)
      loggerMessage.traverse(writer.write(_, Formatter.simple)).unsafeRunSync()
      new String(Files.readAllBytes(Paths.get(fileName))) shouldBe loggerMessage
        .map(Formatter.simple.format)
        .mkString("\n") + (if (loggerMessage.isEmpty) "" else "\n")
    }
  }

  after {
    Files.delete(Paths.get(fileName))
  }
}
