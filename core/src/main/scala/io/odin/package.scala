package io

import cats.effect.{Clock, Concurrent, ContextShift, Resource, Sync, Timer}
import io.odin.formatter.Formatter
import io.odin.loggers.{ConsoleLogger, FileLogger}

package object odin {
  /**
    * Basic console logger that prints to STDOUT & STDERR
    * @param formatter formatter to use for log messages
    */
  def consoleLogger[F[_]: Sync: Clock: ContextShift](formatter: Formatter = Formatter.default): Logger[F] =
    ConsoleLogger(formatter)

  /**
    * Create logger with safe log file allocation suspended inside of `Resource`
    * @param fileName name of log file to append to
    * @param formatter formatter to use
    */
  def fileLogger[F[_]: Concurrent: Timer: ContextShift](
      fileName: String,
      formatter: Formatter = Formatter.default
  ): Resource[F, Logger[F]] = {
    FileLogger(fileName, formatter)
  }
}
