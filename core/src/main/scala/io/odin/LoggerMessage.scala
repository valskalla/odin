package io.odin

import cats.Eval
import io.odin.meta.Position

/**
  * Final log message that contains all the possible information to render
  *
  * @param level log level of the message
  * @param message string message
  * @param context some MDC
  * @param exception exception if exists
  * @param position origin of log
  * @param threadName current thread name
  * @param timestamp Epoch time in milliseconds at the moment of log
  */
case class LoggerMessage(
    level: Level,
    message: Eval[String],
    context: Map[String, String],
    exception: Option[Throwable],
    position: Position,
    threadName: String,
    timestamp: Long
)
