package io.odin.loggers

import java.io.PrintStream

import cats.effect.{Clock, Sync}
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{Level, Logger, LoggerMessage}

case class ConsoleLogger[F[_]: Clock](formatter: Formatter, out: PrintStream, err: PrintStream)(implicit F: Sync[F])
    extends DefaultLogger[F] {
  private def println(out: PrintStream, msg: LoggerMessage, formatter: Formatter): F[Unit] =
    F.delay(out.println(formatter.format(msg)))

  def log(msg: LoggerMessage): F[Unit] =
    if (msg.level < Level.Warn) {
      println(out, msg, formatter)
    } else {
      println(err, msg, formatter)
    }
}

object ConsoleLogger {
  def apply[F[_]: Clock: Sync](formatter: Formatter): Logger[F] =
    ConsoleLogger(formatter, scala.Console.out, scala.Console.err)
}
