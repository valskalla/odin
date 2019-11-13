package io.odin.loggers

import cats.effect.{Clock, Sync}
import cats.kernel.Comparison
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage}
import io.odin.formatter.Formatter
import io.odin.writers.{StdErrLogWriter, StdOutLogWriter}

case class ConsoleLogger[F[_]: Sync: Clock](formatter: Formatter) extends DefaultLogger[F] {

  private val stdOutWriter = StdOutLogWriter[F]
  private val stdErrWriter = StdErrLogWriter[F]

  def log(msg: LoggerMessage): F[Unit] =
    msg.level.comparison(Level.Warn) match {
      case Comparison.LessThan => stdOutWriter.write(msg, formatter)
      case _                   => stdErrWriter.write(msg, formatter)
    }
}

object ConsoleLogger extends ConsoleLoggerBuilder

trait ConsoleLoggerBuilder {

  def consoleLogger[F[_]: Sync: Clock](formatter: Formatter): Logger[F] = ConsoleLogger(formatter)

}
