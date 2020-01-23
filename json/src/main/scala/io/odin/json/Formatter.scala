package io.odin.json

import cats.syntax.show._
import io.circe.syntax._
import io.circe.Encoder
import io.odin.LoggerMessage
import io.odin.formatter.{Formatter => OFormatter}
import io.odin.formatter.Formatter._
import io.odin.formatter.options.ThrowableFormat
import perfolation._

object Formatter {

  val json: OFormatter = create(ThrowableFormat.Default)

  def create(throwableFormat: ThrowableFormat): OFormatter = {
    implicit val encoder: Encoder[LoggerMessage] = loggerMessageEncoder(throwableFormat)
    (msg: LoggerMessage) => msg.asJson.noSpaces
  }

  def loggerMessageEncoder(throwableFormat: ThrowableFormat): Encoder[LoggerMessage] =
    Encoder.forProduct7("level", "message", "context", "exception", "position", "thread_name", "timestamp")(m =>
      (
        m.level.show,
        m.message.value,
        m.context,
        m.exception.map(t => formatThrowable(t, throwableFormat)),
        p"${m.position.enclosureName}:${m.position.line}",
        m.threadName,
        p"${m.timestamp.t.F}T${m.timestamp.t.T}"
      )
    )

}
