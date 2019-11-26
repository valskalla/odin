package io.odin.loggers

import cats.Monad
import cats.effect.Timer
import io.odin.{Logger, LoggerMessage}

case class ConstContextLogger[F[_]: Timer: Monad](ctx: Map[String, String])(inner: Logger[F]) extends DefaultLogger {
  def log(msg: LoggerMessage): F[Unit] = inner.log(msg.copy(context = msg.context ++ ctx))
  override def log(msgs: List[LoggerMessage]): F[Unit] =
    inner.log(msgs.map(msg => msg.copy(context = msg.context ++ ctx)))
}

object ConstContextLogger {
  def withConstContext[F[_]: Timer: Monad](ctx: Map[String, String], inner: Logger[F]): Logger[F] =
    ConstContextLogger(ctx)(inner)
}
