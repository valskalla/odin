package io.odin.config

import cats.Monad
import cats.effect.Timer
import io.odin.Logger

class DefaultBuilder[F[_]: Timer: Monad](withDefault: Logger[F] => Logger[F]) {
  def withNoopFallback: Logger[F] =
    withDefault(Logger.noop[F])
  def withFallback(fallback: Logger[F]): Logger[F] =
    withDefault(fallback)
}
