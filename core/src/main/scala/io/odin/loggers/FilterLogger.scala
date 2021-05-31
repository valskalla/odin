package io.odin.loggers

import cats.Monad
import cats.effect.kernel.Clock
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Filter each `LoggerMessage` using given predicate before passing it to the next logger
  */
case class FilterLogger[F[_]: Clock](fn: LoggerMessage => Boolean, inner: Logger[F])(implicit F: Monad[F])
    extends DefaultLogger[F](inner.minLevel) {
  def submit(msg: LoggerMessage): F[Unit] =
    F.whenA(fn(msg))(inner.log(msg))

  override def submit(msgs: List[LoggerMessage]): F[Unit] =
    inner.log(msgs.filter(fn))

  def withMinimalLevel(level: Level): Logger[F] = copy(inner = inner.withMinimalLevel(level))
}
