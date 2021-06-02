package io.odin.config

import cats.Applicative
import io.odin.Logger

class DefaultBuilder[F[_]: Applicative](val withDefault: Logger[F] => Logger[F]) {
  def withNoopFallback: Logger[F] =
    withDefault(Logger.noop[F])
  def withFallback(fallback: Logger[F]): Logger[F] =
    withDefault(fallback)
}
