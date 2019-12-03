package io.odin.loggers

import cats.Monad
import cats.effect.Timer
import io.odin.{Logger, LoggerMessage}

/**
  * Filter each `LoggerMessage` using given predicate before passing it to the next logger
  */
case class FilterLogger[F[_]: Timer](fn: LoggerMessage => Boolean, inner: Logger[F])(implicit F: Monad[F])
    extends DefaultLogger[F](inner.minLevel) {
  def log(msg: LoggerMessage): F[Unit] =
    F.whenA(fn(msg))(inner.log(msg))

  override def log(msgs: List[LoggerMessage]): F[Unit] =
    inner.log(msgs.filter(fn))
}
