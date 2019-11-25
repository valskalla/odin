package io.odin.json

import cats.syntax.show._
import io.circe.syntax._
import io.circe.Encoder
import io.odin.LoggerMessage
import io.odin.formatter.{Formatter => OFormatter}
import io.odin.formatter.Formatter._
import perfolation._

object Formatter {
  implicit val loggerMessageEncoder: Encoder[LoggerMessage] =
    Encoder.forProduct7("level", "message", "context", "exception", "position", "thread_name", "timestamp")(
      m =>
        (
          m.level.show,
          m.message(),
          m.context,
          m.exception.map(t => formatThrowable(t).toString()),
          p"${m.position.enclosureName}:${m.position.line}",
          m.threadName,
          p"${m.timestamp.t.F}T${m.timestamp.t.T}"
        )
    )

  val json: OFormatter = (msg: LoggerMessage) => msg.asJson.noSpaces
}
