package io

import java.nio.file.OpenOption
import java.time.LocalDateTime
import cats.effect.kernel.{Async, Clock, Resource, Sync}
import io.odin.formatter.Formatter
import io.odin.loggers.{ConsoleLogger, FileLogger, RollingFileLogger}
import io.odin.syntax._

import scala.concurrent.duration._

package object odin {

  /**
    * Basic console logger that prints to STDOUT & STDERR
    * @param formatter formatter to use for log messages
    * @param minLevel minimal level of logs to be printed
    */
  def consoleLogger[F[_]: Sync: Clock](
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Logger[F] =
    ConsoleLogger(formatter, minLevel)

  /**
    * Create logger with safe log file allocation suspended inside of `Resource`
    * @param fileName name of log file to append to
    * @param formatter formatter to use
    * @param minLevel minimal level of logs to be printed
    */
  def fileLogger[F[_]: Sync: Clock](
      fileName: String,
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace,
      openOptions: Seq[OpenOption] = Seq.empty
  ): Resource[F, Logger[F]] = {
    FileLogger(fileName, formatter, minLevel, openOptions)
  }

  /**
    * Create logger with safe log files allocation suspended inside of `Resource`
    *
    * Log files are rotated according to `rolloverInterval` and `maxFileSizeInBytes` parameters. Whenever a log file
    * satisfies at least one of the set requirements, next file is created.
    *
    * Name of each log file is prefixed with `fileNamePrefix` and the current local datetime in format of yyyy-MM-dd-HH-mm-ss
    * is appended to the prefix.
    *
    * In case if none of the options are set, rotation never happens and a single file is used.
    *
    * @param fileNamePattern function that provides a path to a log file given a current local datetime
    * @param rolloverInterval interval for rollover.
    *                         When set, new log file is created each time interval is over
    * @param maxFileSizeInBytes max size of log file.
    *                           When set, new log file is created each time the current log file size exceeds the setting
    * @param formatter formatter to use
    * @param minLevel minimal level of logs to be printed
    */
  def rollingFileLogger[F[_]: Async](
      fileNamePattern: LocalDateTime => String,
      rolloverInterval: Option[FiniteDuration],
      maxFileSizeInBytes: Option[Long],
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace,
      openOptions: Seq[OpenOption] = Seq.empty
  ): Resource[F, Logger[F]] = {
    RollingFileLogger(fileNamePattern, maxFileSizeInBytes, rolloverInterval, formatter, minLevel, openOptions)
  }

  /**
    * Create async logger with safe log file allocation and intermediate async buffer
    * @param fileName name of log file to append to
    * @param formatter formatter to use
    * @param timeWindow pause between async buffer flushing
    * @param maxBufferSize maximum buffer size
    * @param minLevel minimal level of logs to be printed
    */
  def asyncFileLogger[F[_]: Async](
      fileName: String,
      formatter: Formatter = Formatter.default,
      timeWindow: FiniteDuration = 1.second,
      maxBufferSize: Option[Int] = None,
      minLevel: Level = Level.Trace,
      openOptions: Seq[OpenOption] = Seq.empty
  ): Resource[F, Logger[F]] =
    fileLogger[F](fileName, formatter, minLevel, openOptions).withAsync(timeWindow, maxBufferSize)

  /**
    * Same as [[rollingFileLogger]] but with intermediate async buffer
    * @param fileNamePattern function that provides a path to a log file given a current local datetime
    * @param rolloverInterval interval for rollover.
    *                         When set, new log file is created each time interval is over
    * @param maxFileSizeInBytes max size of log file.
    *                           When set, new log file is created each time the current log file size exceeds the setting
    * @param timeWindow pause between async buffer flushing
    * @param maxBufferSize maximum buffer size
    * @param formatter formatter to use
    * @param minLevel minimal level of logs to be printed
    */
  def asyncRollingFileLogger[F[_]: Async](
      fileNamePattern: LocalDateTime => String,
      rolloverInterval: Option[FiniteDuration],
      maxFileSizeInBytes: Option[Long],
      timeWindow: FiniteDuration = 1.second,
      maxBufferSize: Option[Int] = None,
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace,
      openOptions: Seq[OpenOption] = Seq.empty
  ): Resource[F, Logger[F]] =
    rollingFileLogger(fileNamePattern, rolloverInterval, maxFileSizeInBytes, formatter, minLevel, openOptions)
      .withAsync(timeWindow, maxBufferSize)
}
