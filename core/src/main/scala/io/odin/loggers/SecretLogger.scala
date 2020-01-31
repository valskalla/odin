package io.odin.loggers

import java.security.MessageDigest

import cats.Monad
import cats.effect.Clock
import io.odin.util.Hex
import io.odin.{Logger, LoggerMessage}

case class SecretLogger[F[_]: Clock](secrets: Set[String], inner: Logger[F], algo: String = "SHA-1")(
    implicit F: Monad[F]
) extends DefaultLogger[F](inner.minLevel) {
  def log(msg: LoggerMessage): F[Unit] = {
    val md = MessageDigest.getInstance(algo)
    inner.log(
      msg.copy(
        context = msg.context.map {
          case (key, value) =>
            if (secrets.contains(key)) {
              key -> s"secret:${Hex.encodeHex(md.digest(value.getBytes)).take(6)}"
            } else {
              key -> value
            }
        }
      )
    )
  }
}
