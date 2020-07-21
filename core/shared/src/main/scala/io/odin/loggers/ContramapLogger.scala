package io.odin.loggers

import cats.Monad
import cats.effect.Timer
import io.odin.{Logger, LoggerMessage}

/**
  * Apply given function to each `LoggerMessage` before passing it to the next logger
  */
case class ContramapLogger[F[_]: Timer: Monad](f: LoggerMessage => LoggerMessage, inner: Logger[F])
    extends DefaultLogger[F](inner.minLevel) {
  def log(msg: LoggerMessage): F[Unit] = inner.log(f(msg))

  override def log(msgs: List[LoggerMessage]): F[Unit] = inner.log(msgs.map(f))
}
