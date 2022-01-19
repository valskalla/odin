package io.odin.formatter

import cats.syntax.show._
import io.odin.LoggerMessage
import io.odin.formatter.options.{PositionFormat, ThrowableFormat}
import io.odin.meta.Position
import perfolation._

import scala.annotation.tailrec

trait Formatter {
  def format(msg: LoggerMessage): String
}

object Formatter {

  val default: Formatter =
    Formatter.create(ThrowableFormat.Default, PositionFormat.Full, colorful = false, printCtx = true)

  val colorful: Formatter =
    Formatter.create(ThrowableFormat.Default, PositionFormat.Full, colorful = true, printCtx = true)

  def create(throwableFormat: ThrowableFormat, colorful: Boolean): Formatter =
    create(throwableFormat, PositionFormat.Full, colorful, printCtx = true)

  def create(theme: Theme): Formatter =
    create(ThrowableFormat.Default, PositionFormat.Full, theme, colorful = true, printCtx = true)

  /**
    * Creates new formatter with provided options
    *
    * @param throwableFormat @see [[formatThrowable]]
    * @param positionFormat @see [[formatPosition]]
    * @param colorful use different color for thread name, level, position and throwable
    * @param printCtx whether the context is printed in the log
    */
  def create(
      throwableFormat: ThrowableFormat,
      positionFormat: PositionFormat,
      colorful: Boolean,
      printCtx: Boolean
  ): Formatter =
    create(throwableFormat, positionFormat, Theme.ansi, colorful, printCtx)

  /**
    * Creates new formatter with provided options
    *
    * @param throwableFormat @see [[formatThrowable]]
    * @param positionFormat @see [[formatPosition]]
    * @param theme set different colors for ctx, timestamps, thread name, level, position and throwable
    * @param colorful whether to use the colors defined in theme or not
    * @param printCtx whether the context is printed in the log
    */
  def create(
      throwableFormat: ThrowableFormat,
      positionFormat: PositionFormat,
      theme: Theme,
      colorful: Boolean,
      printCtx: Boolean
  ): Formatter = {

    @inline def withColor(color: String, message: String): String =
      if (colorful) s"$color$message${theme.reset}" else message

    (msg: LoggerMessage) => {
      val ctx = if (printCtx) withColor(theme.context, formatCtx(msg.context)) else ""
      val timestamp = withColor(theme.timestamp, formatTimestamp(msg.timestamp))
      val threadName = withColor(theme.threadName, msg.threadName)
      val level = withColor(theme.level, msg.level.show)
      val position = withColor(theme.position, formatPosition(msg.position, positionFormat))

      val throwable = msg.exception match {
        case Some(t) =>
          withColor(theme.exception, s"${System.lineSeparator()}${formatThrowable(t, throwableFormat)}")
        case None =>
          ""
      }

      s"$timestamp [$threadName] $level $position - ${msg.message.value}$ctx$throwable"
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
        builder.append(s"$key: $value")
        if (iterator.hasNext) builder.append(", ")
      }
      builder.toString()
    }

  /**
    * Formats timestamp using the following format: yyyy-MM-ddTHH:mm:ss,SSS
    */
  def formatTimestamp(timestamp: Long): String = {
    val date = timestamp.t
    s"${date.F}T${date.T},${date.milliOfSecond}"
  }

  /**
    * The result differs depending on the format:
    *
    * `PositionFormat.Full` - prints full position
    * 'io.odin.formatter.Formatter formatPosition:75' formatted as 'io.odin.formatter.Formatter formatPosition:75'
    *
    * `PositionFormat.AbbreviatePackage` - prints abbreviated package and full enclosing
    * 'io.odin.formatter.Formatter formatPosition:75' formatted as 'i.o.f.Formatter formatPosition:75'
    */
  def formatPosition(position: Position, format: PositionFormat): String = {
    val lineNumber = if (position.line >= 0) s":${position.line}" else ""

    val enclosure = format match {
      case PositionFormat.Full              => position.enclosureName
      case PositionFormat.AbbreviatePackage => abbreviate(position.enclosureName)

    }

    s"$enclosure$lineNumber"
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

    val filter: Option[Set[String]] = format.filter match {
      case ThrowableFormat.Filter.NoFilter            => None
      case ThrowableFormat.Filter.Excluding(prefixes) => Some(prefixes)
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

      writeStackTrace(builder, depth, filter, t.getStackTrace, indent)
      if (Option(t.getCause).isEmpty) {
        builder
      } else {
        loop(t.getCause, builder)
      }
    }

    loop(t, new StringBuilder).toString()
  }

  private def writeStackTrace(
      b: StringBuilder,
      maybeDepth: Option[Int],
      maybeFilter: Option[Set[String]],
      elements: Array[StackTraceElement],
      indent: String
  ): Unit = {
    val filter = maybeFilter.getOrElse(Set.empty)
    @tailrec
    def write(depth: Int, elements: Array[StackTraceElement]): Unit = {
      elements.headOption match {
        case None                  => // No more elements
        case Some(_) if depth == 0 => // Already printed all
        case Some(head) if filter.nonEmpty && filter.contains(head.getClassName.stripSuffix("$")) => // Need to exclude
          write(depth, elements.tail)
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
          write(depth - 1, elements.tail)
      }
    }

    write(maybeDepth.getOrElse(-1), elements)
  }

  private def abbreviate(enclosure: String): String = {
    @tailrec
    def loop(input: Array[String], builder: StringBuilder): StringBuilder = {
      input.length match {
        case 0 => builder
        case 1 => builder.append(input.head)
        case _ =>
          val head = input.head
          val b = if (head.isEmpty) builder.append(questionMark) else builder.append(head.head)
          loop(input.tail, b.append(dot))
      }
    }

    loop(enclosure.split('.'), new StringBuilder).toString()
  }

  private val questionMark = "?"
  private val dot = "."

}
