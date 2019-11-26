package io.odin

import cats.Monad
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Timer}
import io.odin.loggers.{AsyncLogger, ConstContextLogger, ContextualLogger, RouterLogger, WithContext}

import scala.concurrent.duration._

package object syntax {
  implicit class LoggerSyntax[F[_]](logger: Logger[F]) {
    def withMinimalLevel(level: Level)(implicit clock: Timer[F], monad: Monad[F]): Logger[F] =
      RouterLogger.withMinimalLevel(level, logger)

    def withConstContext(ctx: Map[String, String])(implicit clock: Timer[F], monad: Monad[F]): Logger[F] =
      ConstContextLogger.withConstContext(ctx, logger)

    def withContext(implicit clock: Timer[F], monad: Monad[F], withContext: WithContext[F]): Logger[F] =
      ContextualLogger.withContext(logger)

    def withAsync(
        timeWindow: FiniteDuration = 1.millis,
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: Concurrent[F], contextShift: ContextShift[F]): Resource[F, Logger[F]] =
      AsyncLogger.withAsync(logger, timeWindow, maxBufferSize)

    def withAsyncUnsafe(
        timeWindow: FiniteDuration = 1.millis,
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: ConcurrentEffect[F], contextShift: ContextShift[F]): Logger[F] =
      AsyncLogger.withAsyncUnsafe(logger, timeWindow, maxBufferSize)
  }

  /**
    * Syntax for loggers suspended in `Resource` (i.e. `AsyncLogger` or `FileLogger`)
    */
  implicit class ResourceLoggerSyntax[F[_]](resource: Resource[F, Logger[F]]) {
    def withAsync(
        timeWindow: FiniteDuration = 1.millis,
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: Concurrent[F], contextShift: ContextShift[F]): Resource[F, Logger[F]] =
      resource.flatMap(AsyncLogger.withAsync(_, timeWindow, maxBufferSize))

    def withMinimalLevel(level: Level)(implicit clock: Timer[F], monad: Monad[F]): Resource[F, Logger[F]] =
      resource.map(RouterLogger.withMinimalLevel(level, _))

    def withConstContext(ctx: Map[String, String])(implicit clock: Timer[F], monad: Monad[F]): Resource[F, Logger[F]] =
      resource.map(ConstContextLogger.withConstContext(ctx, _))

    def withContext(implicit clock: Timer[F], monad: Monad[F], withContext: WithContext[F]): Resource[F, Logger[F]] =
      resource.map(ContextualLogger.withContext[F])
  }
}
