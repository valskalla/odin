package io.odin.loggers

import cats.Monad
import cats.effect.{Clock, ContextShift, Sync}
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.writers.{LogWriter, StdErrLogWriter, StdOutLogWriter}
import io.odin.{Level, Logger, LoggerMessage}

case class ConsoleLogger[F[_]: Clock: Monad](formatter: Formatter, out: LogWriter[F], err: LogWriter[F])
    extends DefaultLogger[F] {

  def log(msg: LoggerMessage): F[Unit] =
    if (msg.level < Level.Warn) {
      out.write(msg, formatter)
    } else {
      err.write(msg, formatter)
    }
}
