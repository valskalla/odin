package io.odin.loggers

import cats.Monad
import cats.effect.kernel.Clock
import io.odin.{Level, Logger, LoggerMessage}

case class ConstContextLogger[F[_]: Clock: Monad](ctx: Map[String, String], inner: Logger[F])
    extends DefaultLogger(inner.minLevel) {
  def submit(msg: LoggerMessage): F[Unit] = inner.log(msg.copy(context = msg.context ++ ctx))
  override def submit(msgs: List[LoggerMessage]): F[Unit] =
    inner.log(msgs.map(msg => msg.copy(context = msg.context ++ ctx)))

  def withMinimalLevel(level: Level): Logger[F] = copy(inner = inner.withMinimalLevel(level))
}

object ConstContextLogger {
  def withConstContext[F[_]: Clock: Monad](ctx: Map[String, String], inner: Logger[F]): Logger[F] =
    ConstContextLogger(ctx, inner)
}
