package io.odin.extras

import cats.effect.{Concurrent, ContextShift, Resource, Timer}
import io.odin.extras.loggers.ConditionalLogger
import io.odin.{Level, Logger}

package object syntax {

  implicit class LoggerExtraSyntax[F[_]](logger: Logger[F]) {

    /**
      * Create conditional logger that buffers messages and sends them to the inner logger when the resource is released.
      * @param minLevelOnError min log level that will be used in case of error
      * @param maxBufferSize max logs buffer size
      * @return Logger suspended in [[Resource]]. Once resource is released, the messages are being sent to the inner logger.
      */
    def withErrorLevel(
        minLevelOnError: Level,
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: Concurrent[F], contextShift: ContextShift[F]): Resource[F, Logger[F]] =
      ConditionalLogger.create[F](logger, minLevelOnError, maxBufferSize)

  }

}
