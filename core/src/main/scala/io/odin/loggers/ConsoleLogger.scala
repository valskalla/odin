package io.odin.loggers

import cats.Monad
import cats.effect.{Clock, ContextShift, Sync}
import cats.kernel.Comparison
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage}
import io.odin.formatter.Formatter
import io.odin.writers.{LogWriter, StdErrLogWriter, StdOutLogWriter}

case class ConsoleLogger[F[_]: Clock: Monad](formatter: Formatter, out: LogWriter[F], err: LogWriter[F])
    extends DefaultLogger[F] {

  def log(msg: LoggerMessage): F[Unit] =
    msg.level.comparison(Level.Warn) match {
      case Comparison.LessThan => out.write(msg, formatter)
      case _                   => err.write(msg, formatter)
    }
}

object ConsoleLogger extends ConsoleLoggerBuilder

trait ConsoleLoggerBuilder {

  def consoleLogger[F[_]: Sync: Clock: ContextShift](formatter: Formatter): Logger[F] =
    ConsoleLogger(formatter, StdOutLogWriter[F](), StdErrLogWriter[F]())

}
