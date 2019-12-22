package io.odin.formatter

import cats.syntax.show._
import io.odin.LoggerMessage
import perfolation._

import scala.annotation.tailrec

trait Formatter {
  def format(msg: LoggerMessage): String
}

object Formatter {
  val default: Formatter = (msg: LoggerMessage) => {
    val ctx = formatCtx(msg.context)
    val lineNumber = if (msg.position.line >= 0) p":${msg.position.line}" else ""
    msg.exception match {
      case Some(t) =>
        val formattedThrowable = formatThrowable(t)
        p"${msg.timestamp.t.F}T${msg.timestamp.t.T} [${msg.threadName}] ${msg.level.show} ${msg.position.enclosureName}$lineNumber - ${msg.message.value} $ctx${System
          .lineSeparator()}${formattedThrowable.toString}"
      case None =>
        p"${msg.timestamp.t.F}T${msg.timestamp.t.T} [${msg.threadName}] ${msg.level.show} ${msg.position.enclosureName}$lineNumber - ${msg.message.value}$ctx"
    }
  }

  def formatCtx(context: Map[String, String]): String =
    if (context.isEmpty) {
      ""
    } else {
      val builder = new StringBuilder(" - ")
      val iterator = context.iterator
      while (iterator.hasNext) {
        val (key, value) = iterator.next()
        builder.append(p"$key: $value")
        if (iterator.hasNext) builder.append(", ")
      }
      builder.toString()
    }

  /**
    * Default Throwable printer is twice as slow. This method was borrowed from scribe library.
    */
  def formatThrowable(t: Throwable, builder: StringBuilder = new StringBuilder): StringBuilder = {
    builder.append("Caused by: ")
    builder.append(t.getClass.getName)
    if (Option(t.getLocalizedMessage).isDefined) {
      builder.append(": ")
      builder.append(t.getLocalizedMessage)
    }
    builder.append(System.lineSeparator())
    writeStackTrace(builder, t.getStackTrace)
    if (Option(t.getCause).isEmpty) {
      builder
    } else {
      formatThrowable(t.getCause, builder)
    }
  }

  @tailrec
  private def writeStackTrace(b: StringBuilder, elements: Array[StackTraceElement]): Unit = {
    elements.headOption match {
      case None => // No more elements
      case Some(head) =>
        b.append(head.getClassName)
        b.append('.')
        b.append(head.getMethodName)
        b.append('(')
        b.append(head.getFileName)
        if (head.getLineNumber > 0) {
          b.append(':')
          b.append(head.getLineNumber)
        }
        b.append(')')
        b.append(System.lineSeparator())
        writeStackTrace(b, elements.tail)
    }
  }
}
