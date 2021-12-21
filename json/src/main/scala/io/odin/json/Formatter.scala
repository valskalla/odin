package io.odin.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.odin.LoggerMessage
import io.odin.formatter.{Formatter => OFormatter}
import io.odin.formatter.Formatter._
import io.odin.formatter.options.{PositionFormat, ThrowableFormat}

object Formatter {

  val json: OFormatter = create(ThrowableFormat.Default, PositionFormat.Full)

  def create(throwableFormat: ThrowableFormat): OFormatter =
    create(throwableFormat, PositionFormat.Full)

  def create(throwableFormat: ThrowableFormat, positionFormat: PositionFormat): OFormatter = { (msg: LoggerMessage) =>
    writeToString(
      Output(
        msg.level,
        msg.message.value,
        msg.context,
        msg.exception.map(t => formatThrowable(t, throwableFormat)),
        formatPosition(msg.position, positionFormat),
        msg.threadName,
        formatTimestamp(msg.timestamp)
      )
    )
  }
}
