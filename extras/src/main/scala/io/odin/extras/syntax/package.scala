package io.odin.extras

import cats.effect.kernel.Async
import io.odin.extras.loggers.ConditionalLogger
import io.odin.{Level, Logger}

package object syntax {

  implicit class LoggerExtraSyntax[F[_]](private val logger: Logger[F]) extends AnyVal {

    /**
      * Evaluate the `use` statement using conditional logger that buffers messages and sends them to the inner logger
      * when the evaluation is completed.
      *
      * @param minLevelOnError min log level that will be used in case of error
      * @param maxBufferSize max logs buffer size
      */
    def withErrorLevel[A](
        minLevelOnError: Level,
        maxBufferSize: Option[Int] = None
    )(use: Logger[F] => F[A])(implicit F: Async[F]): F[A] =
      ConditionalLogger.create[F](logger, minLevelOnError, maxBufferSize).use(use)

  }

}
