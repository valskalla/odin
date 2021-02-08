package io.odin.loggers

import cats.Monad
import cats.effect.Clock
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Apply given function to each `LoggerMessage` before passing it to the next logger
  */
case class ContramapLogger[F[_]: Clock: Monad](f: LoggerMessage => LoggerMessage, inner: Logger[F])
    extends DefaultLogger[F](inner.minLevel) {
  def submit(msg: LoggerMessage): F[Unit] = inner.log(f(msg))

  override def submit(msgs: List[LoggerMessage]): F[Unit] = inner.log(msgs.map(f))

  def withMinimalLevel(level: Level): Logger[F] = copy(inner = inner.withMinimalLevel(level))
}
