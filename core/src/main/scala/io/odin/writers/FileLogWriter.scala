package io.odin.writers

import java.io.BufferedWriter
import java.nio.file.{Files, Paths}

import cats.effect.Sync
import io.odin.LoggerMessage
import io.odin.formatter.Formatter

class FileLogWriter[F[_]](writer: BufferedWriter)(implicit F: Sync[F]) extends LogWriter[F] {

  Runtime.getRuntime.addShutdownHook {
    new Thread {
      override def run(): Unit =
        writer.close()
    }
  }

  def write(msg: LoggerMessage, formatter: Formatter): F[Unit] = {
    F.guarantee {
      F.delay {
        writer.write(formatter.format(msg))
        writer.write(System.lineSeparator())
      }
    }(flush)
  }

  def flush: F[Unit] = F.handleErrorWith(F.delay(writer.flush()))(_ => F.unit)
}

object FileLogWriter {

  def apply[F[_]: Sync](fileName: String): LogWriter[F] =
    new FileLogWriter[F](Files.newBufferedWriter(Paths.get(fileName)))

}
