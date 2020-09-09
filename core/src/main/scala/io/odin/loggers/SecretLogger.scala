package io.odin.loggers

import java.security.MessageDigest

import io.odin.LoggerMessage
import io.odin.util.Hex

object SecretLogger {

  def apply(secrets: Set[String], algo: String = "SHA-1")(msg: LoggerMessage): LoggerMessage = {
    val md = MessageDigest.getInstance(algo)
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
  }

}
