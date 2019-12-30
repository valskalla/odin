package io.odin

import cats.Monad
import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Timer}
import io.odin.loggers.{AsyncLogger, ConstContextLogger, ContextualLogger, ContramapLogger, FilterLogger, WithContext}

import scala.concurrent.duration._

package object syntax {
  implicit class LoggerSyntax[F[_]](logger: Logger[F]) {

    /**
      * Create logger that adds constant context to each log record
      * @param ctx constant context
      */
    def withConstContext(ctx: Map[String, String])(implicit clock: Timer[F], monad: Monad[F]): Logger[F] =
      ConstContextLogger.withConstContext(ctx, logger)

    /**
      * Create contextual logger that is capable of picking up context from inside of `F[_]`.
      * See `ContextualLogger` for more info
      */
    def withContext(implicit clock: Timer[F], monad: Monad[F], withContext: WithContext[F]): Logger[F] =
      ContextualLogger.withContext(logger)

    /**
      * Create async logger that buffers the messages up to the limit (if any) and flushes it down the chain each `timeWindow`
      * @param timeWindow pause between async buffer flushing
      * @param maxBufferSize max logs buffer size
      * @return Logger suspended in `Resource`. Once this `Resource` started, internal flush loop is initialized. Once
      *         resource is released, flushing is also stopped.
      */
    def withAsync(
        timeWindow: FiniteDuration = 1.millis,
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: Concurrent[F], contextShift: ContextShift[F]): Resource[F, Logger[F]] =
      AsyncLogger.withAsync(logger, timeWindow, maxBufferSize)

    /**
      * Create and unsafely run async logger that buffers the messages up to the limit (if any) and
      * flushes it down the chain each `timeWindow`
      * @param timeWindow  pause between async buffer flushing
      * @param maxBufferSize max logs buffer size
      */
    def withAsyncUnsafe(
        timeWindow: FiniteDuration = 1.millis,
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: ConcurrentEffect[F], contextShift: ContextShift[F]): Logger[F] =
      AsyncLogger.withAsyncUnsafe(logger, timeWindow, maxBufferSize)

    /**
      * Modify logger message before it's written to the logger
      */
    def contramap(f: LoggerMessage => LoggerMessage)(implicit timer: Timer[F], F: Monad[F]): Logger[F] =
      ContramapLogger(f, logger)

    /**
      * Filter messages given the predicate. Falsified cases are dropped from the logging
      */
    def filter(f: LoggerMessage => Boolean)(implicit timer: Timer[F], F: Monad[F]): Logger[F] =
      FilterLogger(f, logger)
  }

  /**
    * Syntax for loggers suspended in `Resource` (i.e. `AsyncLogger` or `FileLogger`)
    */
  implicit class ResourceLoggerSyntax[F[_]](resource: Resource[F, Logger[F]]) {

    /**
      * Create async logger that buffers the messages up to the limit (if any) and flushes it down the chain each `timeWindow`
      * @param timeWindow pause between async buffer flushing
      * @param maxBufferSize max logs buffer size
      * @return Logger suspended in `Resource`. Once this `Resource` started, internal flush loop is initialized. Once
      *         resource is released, flushing is also stopped.
      */
    def withAsync(
        timeWindow: FiniteDuration = 1.millis,
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: Concurrent[F], contextShift: ContextShift[F]): Resource[F, Logger[F]] =
      resource.flatMap(AsyncLogger.withAsync(_, timeWindow, maxBufferSize))

    /**
      * Create logger that adds constant context to each log record
      * @param ctx constant context
      */
    def withConstContext(ctx: Map[String, String])(implicit clock: Timer[F], monad: Monad[F]): Resource[F, Logger[F]] =
      resource.map(ConstContextLogger.withConstContext(ctx, _))

    /**
      * Create contextual logger that is capable of picking up context from inside of `F[_]`.
      * See `ContextualLogger` for more info
      */
    def withContext(implicit clock: Timer[F], monad: Monad[F], withContext: WithContext[F]): Resource[F, Logger[F]] =
      resource.map(ContextualLogger.withContext[F])

    /**
      * Intercept logger message before it's written to the logger
      */
    def contramap(f: LoggerMessage => LoggerMessage)(implicit timer: Timer[F], F: Monad[F]): Resource[F, Logger[F]] =
      resource.map(ContramapLogger(f, _))

    /**
      * Filter messages given the predicate. Falsified cases are dropped from the logging
      */
    def filter(f: LoggerMessage => Boolean)(implicit timer: Timer[F], F: Monad[F]): Resource[F, Logger[F]] =
      resource.map(FilterLogger(f, _))
  }
}
