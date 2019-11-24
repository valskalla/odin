package io.odin

import cats.Monad
import cats.effect.Clock
import io.odin.loggers.{ConstContextLogger, ContextualLogger, RouterLogger, WithContext}

package object syntax {

  implicit class LoggerSyntax[F[_]](logger: Logger[F]) {

    def withMinimalLevel(level: Level)(implicit clock: Clock[F], monad: Monad[F]): Logger[F] =
      RouterLogger.withMinimalLevel(level)(logger)

    def withConstContext(ctx: Map[String, String])(implicit clock: Clock[F], monad: Monad[F]): Logger[F] =
      ConstContextLogger.withConstContext(ctx)(logger)

    def withContextualLogger(implicit clock: Clock[F], monad: Monad[F], withContext: WithContext[F]): Logger[F] =
      ContextualLogger.withContextualLogger.apply(logger)

  }

}
