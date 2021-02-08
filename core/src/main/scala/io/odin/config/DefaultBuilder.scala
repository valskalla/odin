package io.odin.config

import cats.Monad
import cats.effect.Clock
import io.odin.Logger

class DefaultBuilder[F[_]: Clock: Monad](val withDefault: Logger[F] => Logger[F]) {
  def withNoopFallback: Logger[F] =
    withDefault(Logger.noop[F])
  def withFallback(fallback: Logger[F]): Logger[F] =
    withDefault(fallback)
}
