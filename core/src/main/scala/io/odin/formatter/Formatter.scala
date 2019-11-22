package io.odin.formatter

import java.io.{PrintWriter, StringWriter}

import cats.syntax.show._
import io.odin.LoggerMessage
import perfolation._

trait Formatter {

  def format(msg: LoggerMessage): String

}

object Formatter {

  val simple: Formatter = (msg: LoggerMessage) => p"${msg.toString}. Message: ${msg.message()}"
  val default: Formatter = (msg: LoggerMessage) => {
    msg.exception match {
      case Some(t) =>
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        t.printStackTrace(pw)
        p"${msg.timestamp.t.F} [${msg.threadName}] ${msg.level.show} ${msg.position.enclosureName}:${msg.position.line} - ${msg
          .message()}${System.lineSeparator()}${sw.toString}"
      case None =>
        p"${msg.timestamp.t.F} [${msg.threadName}] ${msg.level.show} ${msg.position.enclosureName}:${msg.position.line} - ${msg.message()}"
    }
  }

}
