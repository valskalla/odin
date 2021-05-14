package io.odin.loggers

import java.io.BufferedWriter
import java.nio.file.{Files, OpenOption, Paths}
import cats.effect.kernel.{Resource, Sync}
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Write to given log writer with provided formatter
  */
case class FileLogger[F[_]](buffer: BufferedWriter, formatter: Formatter, override val minLevel: Level)(
    implicit F: Sync[F]
) extends DefaultLogger[F](minLevel) {
  def submit(msg: LoggerMessage): F[Unit] =
    F.guarantee(write(msg, formatter), flush)

  override def submit(msgs: List[LoggerMessage]): F[Unit] =
    F.guarantee(msgs.traverse(write(_, formatter)).void, flush)

  private def write(msg: LoggerMessage, formatter: Formatter): F[Unit] =
    F.delay {
      buffer.write(formatter.format(msg) + System.lineSeparator())
    }

  private def flush: F[Unit] = F.delay(buffer.flush()).handleErrorWith(_ => F.unit)

  def withMinimalLevel(level: Level): Logger[F] = copy(minLevel = level)
}

object FileLogger {
  def apply[F[_]](
      fileName: String,
      formatter: Formatter,
      minLevel: Level,
      openOptions: Seq[OpenOption] = Seq.empty
  )(
      implicit F: Sync[F]
  ): Resource[F, Logger[F]] = {
    def mkDirs: F[Unit] = F.delay {
      Option(Paths.get(fileName).getParent).foreach(_.toFile.mkdirs())
    }
    def mkBuffer: F[BufferedWriter] = F.delay(Files.newBufferedWriter(Paths.get(fileName), openOptions: _*))
    def closeBuffer(buffer: BufferedWriter): F[Unit] =
      F.delay(buffer.close()).handleErrorWith(_ => F.unit)

    Resource.make(mkDirs >> mkBuffer)(closeBuffer).map { buffer =>
      FileLogger(buffer, formatter, minLevel)
    }
  }
}
