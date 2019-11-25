package io.odin.loggers

import cats.Monad
import cats.effect.Clock
import io.odin.LoggerMessage
import io.odin.formatter.Formatter
import io.odin.writers.LogWriter

/**
  * Write to given log writer with provided formatter
  */
case class FileLogger[F[_]: Clock: Monad](logWriter: LogWriter[F], formatter: Formatter) extends DefaultLogger[F] {
  def log(msg: LoggerMessage): F[Unit] = logWriter.write(msg, formatter)
}
