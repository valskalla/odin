package io.odin.slf4j

import io.odin.Level
import org.slf4j.helpers.MessageFormatter

trait OdinLoggerVarargsAdapter[F[_]] { self: OdinLoggerAdapter[F] =>

  def trace(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Trace, MessageFormatter.arrayFormat(format, arguments.toArray))

  def debug(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Debug, MessageFormatter.arrayFormat(format, arguments.toArray))

  def info(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Info, MessageFormatter.arrayFormat(format, arguments.toArray))

  def warn(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Warn, MessageFormatter.arrayFormat(format, arguments.toArray))

  def error(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Error, MessageFormatter.arrayFormat(format, arguments.toArray))

}
