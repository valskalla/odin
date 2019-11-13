package io.odin.loggers

import cats.effect.{Clock, Concurrent}
import io.odin.{Logger, LoggerMessage}

/**
  * AsyncLogger spawns non-cancellable `cats.effect.Fiber` with actual log action encapsulated there
  */
//@TODO probably replace with monix queue instead to have control over the buffer
case class AsyncLogger[F[_]: Clock](inner: Logger[F])(implicit F: Concurrent[F]) extends DefaultLogger[F] {
  def log(msg: LoggerMessage): F[Unit] = F.void(F.start(inner.log(msg)))
}

object AsyncLogger extends AsyncLoggerBuilder

trait AsyncLoggerBuilder {

  def withAsync[F[_]: Clock: Concurrent]: Logger[F] => Logger[F] = AsyncLogger.apply

}
