package io.odin.loggers

import cats.Monad
import cats.effect.Clock
import io.odin.{Logger, LoggerMessage}

case class ConstContextLogger[F[_]: Clock: Monad](ctx: Map[String, String])(inner: Logger[F]) extends DefaultLogger {
  def log(msg: LoggerMessage): F[Unit] = inner.log(msg.copy(context = msg.context ++ ctx))
}

object ConstContextLogger extends ConstContextLoggerBuilder

trait ConstContextLoggerBuilder {

  def withConstContext[F[_]: Clock: Monad](ctx: Map[String, String])(inner: Logger[F]): Logger[F] =
    ConstContextLogger(ctx)(inner)

}
