package io.odin

import java.nio.file.{Files, Paths}
import java.util.UUID

import cats.effect.{IO, IOApp}
import cats.syntax.all._
import io.odin.syntax._

object Test extends IOApp.Simple {

  val fileName: String = Files.createTempFile(UUID.randomUUID().toString, "").toAbsolutePath.toString

  val message: String = "msg"

  def run: IO[Unit] =
    fileLogger[IO](fileName)
      .withAsync(maxBufferSize = Some(1000000))
      .use { logger =>
        val io = (1 to 1000).toList.traverse(_ => logger.info(message))
        io.foreverM
      }
      .guarantee(IO.delay(Files.delete(Paths.get(fileName))))
}
