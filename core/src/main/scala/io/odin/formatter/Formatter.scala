package io.odin.formatter

import cats.syntax.show._
import io.odin.LoggerMessage
import io.odin.formatter.options.ThrowableFormat
import perfolation._

import scala.annotation.tailrec
import scala.io.AnsiColor._

trait Formatter {
  def format(msg: LoggerMessage): String
}

object Formatter {

  val BRIGHT_BLACK = "\u001b[30;1m"

  val default: Formatter = Formatter.create(ThrowableFormat.Default, colorful = false)

  val colorful: Formatter = Formatter.create(ThrowableFormat.Default, colorful = true)

  /**
    * Creates new formatter with provided options
    *
    * @param throwableFormat @see [[formatThrowable]]
    * @param colorful use different color for thread name, level, position and throwable
    */
  def create(throwableFormat: ThrowableFormat, colorful: Boolean): Formatter = {

    @inline def withColor(color: String, message: String): String =
      if (colorful) p"$color$message$RESET" else message

    (msg: LoggerMessage) => {
      val ctx = withColor(MAGENTA, formatCtx(msg.context))
      val timestamp = p"${msg.timestamp.t.F}T${msg.timestamp.t.T},${msg.timestamp.t.milliOfSecond}"
      val threadName = withColor(GREEN, msg.threadName)
      val level = withColor(BRIGHT_BLACK, msg.level.show)

      val position = {
        val lineNumber = if (msg.position.line >= 0) p":${msg.position.line}" else ""
        withColor(BLUE, p"${msg.position.enclosureName}$lineNumber")
      }

      val throwable = msg.exception match {
        case Some(t) =>
          withColor(RED, p"${System.lineSeparator()}${formatThrowable(t, throwableFormat)}")
        case None =>
          ""
      }

      p"$timestamp [$threadName] $level $position - ${msg.message.value}$ctx$throwable"
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
    *
    * The result differs depending on the format:
    * `ThrowableFormat.Depth.Full` - prints all elements of a stack trace
    * `ThrowableFormat.Depth.Fixed` - prints N elements of a stack trace
    * `ThrowableFormat.Indent.NoIndent` - prints a stack trace without indentation
    * `ThrowableFormat.Indent.Fixed` - prints a stack trace prepending every line with N spaces
    */
  def formatThrowable(t: Throwable, format: ThrowableFormat): String = {
    val indent = format.indent match {
      case ThrowableFormat.Indent.NoIndent    => ""
      case ThrowableFormat.Indent.Fixed(size) => "".padTo(size, ' ')
    }

    val depth: Option[Int] = format.depth match {
      case ThrowableFormat.Depth.Full        => None
      case ThrowableFormat.Depth.Fixed(size) => Some(size)
    }

    @tailrec
    def loop(t: Throwable, builder: StringBuilder): StringBuilder = {
      builder.append("Caused by: ")
      builder.append(t.getClass.getName)
      if (Option(t.getLocalizedMessage).isDefined) {
        builder.append(": ")
        builder.append(t.getLocalizedMessage)
      }
      builder.append(System.lineSeparator())
      writeStackTrace(builder, depth.fold(t.getStackTrace)(t.getStackTrace.take), indent)
      if (Option(t.getCause).isEmpty) {
        builder
      } else {
        loop(t.getCause, builder)
      }
    }

    loop(t, new StringBuilder).toString()
  }

  @tailrec
  private def writeStackTrace(b: StringBuilder, elements: Array[StackTraceElement], indent: String): Unit = {
    elements.headOption match {
      case None => // No more elements
      case Some(head) =>
        b.append(indent)
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
        writeStackTrace(b, elements.tail, indent)
    }
  }
}
