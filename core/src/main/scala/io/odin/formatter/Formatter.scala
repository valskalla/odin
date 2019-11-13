package io.odin.formatter

import io.odin.LoggerMessage

trait Formatter {

  def format(msg: LoggerMessage): String

}

object Formatter {

  val simple: Formatter = (msg: LoggerMessage) => s"${msg.toString}. Message: ${msg.message()}"

}
