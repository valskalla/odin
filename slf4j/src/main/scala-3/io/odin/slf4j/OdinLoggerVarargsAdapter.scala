package io.odin.slf4j

import io.odin.Level
import org.slf4j.helpers.MessageFormatter

import scala.annotation.varargs

trait OdinLoggerVarargsAdapter[F[_]] { self: OdinLoggerAdapter[F] =>

  @varargs def trace(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Trace, MessageFormatter.arrayFormat(format, arguments.toArray))

  @varargs def debug(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Debug, MessageFormatter.arrayFormat(format, arguments.toArray))

  @varargs def info(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Info, MessageFormatter.arrayFormat(format, arguments.toArray))

  @varargs def warn(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Warn, MessageFormatter.arrayFormat(format, arguments.toArray))

  @varargs def error(format: String, arguments: AnyRef*): Unit =
    runFormatted(Level.Error, MessageFormatter.arrayFormat(format, arguments.toArray))

}
