package io

import cats.effect.{Concurrent, ContextShift, Resource, Sync, Timer}
import io.odin.formatter.Formatter
import io.odin.loggers.{ConsoleLogger, FileLogger}
import io.odin.syntax._

import scala.concurrent.duration._

package object odin {
  /**
    * Basic console logger that prints to STDOUT & STDERR
    * @param formatter formatter to use for log messages
    */
  def consoleLogger[F[_]: Sync: Timer: ContextShift](formatter: Formatter = Formatter.default): Logger[F] =
    ConsoleLogger(formatter)

  /**
    * Create logger with safe log file allocation suspended inside of `Resource`
    * @param fileName name of log file to append to
    * @param formatter formatter to use
    */
  def fileLogger[F[_]: Sync: Timer](
      fileName: String,
      formatter: Formatter = Formatter.default
  ): Resource[F, Logger[F]] = {
    FileLogger(fileName, formatter)
  }

  /**
    * Create async logger with safe log file allocation and intermediate async buffer
    * @param fileName name of log file to append to
    * @param formatter formatter to use
    * @param timeWindow pause between async buffer flushing
    * @param maxBufferSize maximum buffer size
    */
  def asyncFileLogger[F[_]: Concurrent: Timer: ContextShift](
      fileName: String,
      formatter: Formatter = Formatter.default,
      timeWindow: FiniteDuration = 1.second,
      maxBufferSize: Option[Int] = None
  ): Resource[F, Logger[F]] =
    fileLogger[F](fileName, formatter).withAsync(timeWindow, maxBufferSize)
}
