package io.odin.loggers

import java.io.BufferedWriter
import java.nio.file.{Files, Paths}

import cats.effect.syntax.all._
import cats.effect.{Resource, Sync, Timer}
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Write to given log writer with provided formatter
  */
case class FileLogger[F[_]: Timer](buffer: BufferedWriter, formatter: Formatter, override val minLevel: Level)(
    implicit F: Sync[F]
) extends DefaultLogger[F](minLevel) {
  def log(msg: LoggerMessage): F[Unit] =
    write(msg, formatter).guarantee(flush)

  override def log(msgs: List[LoggerMessage]): F[Unit] =
    msgs.traverse(write(_, formatter)).void.guarantee(flush)

  private def write(msg: LoggerMessage, formatter: Formatter): F[Unit] =
    F.delay {
      buffer.write(formatter.format(msg) + System.lineSeparator())
    }

  private def flush: F[Unit] = F.delay(buffer.flush()).handleErrorWith(_ => F.unit)
}

object FileLogger {
  def apply[F[_]: Timer](fileName: String, formatter: Formatter, minLevel: Level)(
      implicit F: Sync[F]
  ): Resource[F, Logger[F]] = {
    def mkBuffer: F[BufferedWriter] = F.delay(Files.newBufferedWriter(Paths.get(fileName)))
    def closeBuffer(buffer: BufferedWriter): F[Unit] =
      F.delay(buffer.close()).handleErrorWith(_ => F.unit)

    Resource.make(mkBuffer)(closeBuffer).map { buffer =>
      FileLogger(buffer, formatter, minLevel)
    }
  }
}
