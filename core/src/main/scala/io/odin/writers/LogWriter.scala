package io.odin.writers

import cats.Applicative
import cats.syntax.all._
import cats.kernel.Monoid
import io.odin.LoggerMessage
import io.odin.formatter.Formatter

trait LogWriter[F[_]] {

  def write(msg: LoggerMessage, formatter: Formatter): F[Unit]

}

object LogWriter {

  def noop[F[_]](implicit F: Applicative[F]): LogWriter[F] = (_: LoggerMessage, _: Formatter) => F.unit

  implicit def monoidLogWriter[F[_]: Applicative]: Monoid[LogWriter[F]] = new Monoid[LogWriter[F]] {
    val empty: LogWriter[F] = LogWriter.noop

    def combine(x: LogWriter[F], y: LogWriter[F]): LogWriter[F] =
      (msg: LoggerMessage, formatter: Formatter) => x.write(msg, formatter) *> y.write(msg, formatter)
  }

}
