package io.odin.loggers

import cats.effect.{Clock, Concurrent}
import io.odin.{Logger, LoggerMessage}

/**
  * AsyncLogger spawns non-cancellable `cats.effect.Fiber` with actual log action encapsulated there
  */
//@TODO probably replace with monix queue instead to have control over the buffer
class AsyncLogger[F[_]](inner: Logger[F])(implicit clock: Clock[F], F: Concurrent[F]) extends DefaultLogger[F] {
  def log(msg: LoggerMessage): F[Unit] = F.void(F.start(inner.log(msg)))
}
