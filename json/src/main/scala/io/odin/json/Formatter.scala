package io.odin.json

import cats.syntax.show._
import io.circe.syntax._
import io.circe.Encoder
import io.odin.LoggerMessage
import io.odin.formatter.{Formatter => OFormatter}
import io.odin.formatter.Formatter._
import io.odin.formatter.options.{PositionFormat, ThrowableFormat}

object Formatter {

  val json: OFormatter = create(ThrowableFormat.Default, PositionFormat.Full)

  def create(throwableFormat: ThrowableFormat, positionFormat: PositionFormat): OFormatter = {
    implicit val encoder: Encoder[LoggerMessage] = loggerMessageEncoder(throwableFormat, positionFormat)
    (msg: LoggerMessage) => msg.asJson.noSpaces
  }

  def loggerMessageEncoder(throwableFormat: ThrowableFormat, positionFormat: PositionFormat): Encoder[LoggerMessage] =
    Encoder.forProduct7("level", "message", "context", "exception", "position", "thread_name", "timestamp")(m =>
      (
        m.level.show,
        m.message.value,
        m.context,
        m.exception.map(t => formatThrowable(t, throwableFormat)),
        formatPosition(m.position, positionFormat),
        m.threadName,
        formatTimestamp(m.timestamp)
      )
    )

}
