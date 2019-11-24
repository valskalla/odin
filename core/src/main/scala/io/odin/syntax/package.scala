package io.odin

import cats.Monad
import cats.effect.{Clock, Concurrent, ConcurrentEffect, ContextShift, Timer}
import io.odin.loggers.{AsyncLogger, ConstContextLogger, ContextualLogger, RouterLogger, WithContext}

package object syntax {

  implicit class LoggerSyntax[F[_]](logger: Logger[F]) {

    def withMinimalLevel(level: Level)(implicit clock: Clock[F], monad: Monad[F]): Logger[F] =
      RouterLogger.withMinimalLevel(level, logger)

    def withConstContext(ctx: Map[String, String])(implicit clock: Clock[F], monad: Monad[F]): Logger[F] =
      ConstContextLogger.withConstContext(ctx, logger)

    def withContext(implicit clock: Clock[F], monad: Monad[F], withContext: WithContext[F]): Logger[F] =
      ContextualLogger.withContext(logger)

    def withAsync(
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: Concurrent[F], contextShift: ContextShift[F]): F[Logger[F]] =
      AsyncLogger.withAsync(maxBufferSize, logger)

    def withAsyncUnsafe(
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: ConcurrentEffect[F], contextShift: ContextShift[F]): Logger[F] =
      AsyncLogger.withAsyncUnsafe(maxBufferSize, logger)
  }

}
