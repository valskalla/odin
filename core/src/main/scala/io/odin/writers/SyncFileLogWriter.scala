package io.odin.writers

import java.io.BufferedWriter
import java.nio.file.{Files, Paths, StandardOpenOption}

import cats.effect.Sync
import io.odin.LoggerMessage
import io.odin.formatter.Formatter

/**
  * Sync file logger that always flushes to the file after log is written.
  *
  * Performance-wise, it's better to use [[AsyncFileLogWriter]] version that buffers the events before flushing to disk
  */
class SyncFileLogWriter[F[_]](writer: BufferedWriter)(implicit F: Sync[F]) extends LogWriter[F] {
  Runtime.getRuntime.addShutdownHook {
    new Thread {
      override def run(): Unit =
        writer.close()
    }
  }

  def write(msg: LoggerMessage, formatter: Formatter): F[Unit] = {
    F.guarantee {
      F.delay {
        writer.write(formatter.format(msg) + System.lineSeparator())
      }
    }(flush)
  }

  def flush: F[Unit] = F.handleErrorWith(F.delay(writer.flush()))(_ => F.unit)
}

object SyncFileLogWriter {
  def apply[F[_]: Sync](fileName: String): LogWriter[F] =
    new SyncFileLogWriter[F](Files.newBufferedWriter(Paths.get(fileName), StandardOpenOption.APPEND))
}
