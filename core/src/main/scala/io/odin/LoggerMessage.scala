package io.odin

import io.odin.meta.Position

/**
  * Final log message that contains all the possible information to render
 *
 * @param level log level of the message
  * @param message string message
  * @param context some MDC
  * @param exception exception if exists
  * @param position origin of log
  * @param thread current thread
  * @param timestamp Epoch time in milliseconds at the moment of log
  */
case class LoggerMessage(
    level: Level,
    message: () => String,
    context: Map[String, String],
    exception: Option[Throwable],
    position: Position,
    thread: Thread,
    timestamp: Long
)
