package io

import cats.effect.{Clock, Concurrent, ConcurrentEffect, ContextShift, Sync, Timer}
import io.odin.formatter.Formatter
import io.odin.loggers.{ConsoleLogger, FileLogger}
import io.odin.writers.{AsyncFileLogWriter, StdErrLogWriter, StdOutLogWriter}
import scala.concurrent.duration._
import cats.syntax.all._

import scala.concurrent.duration.FiniteDuration

package object odin {

  /**
    * Basic console logger that prints to STDOUT & STDERR
    * @param formatter formatter to use for log messages
    */
  def consoleLogger[F[_]: Sync: Clock: ContextShift](formatter: Formatter = Formatter.default): Logger[F] =
    ConsoleLogger(formatter, StdOutLogWriter[F], StdErrLogWriter[F])

  /**
    * Safely start async file logger. Cancellation of `F[_]` will be propagated down the chain to safely close the buffer.
    * BEWARE that cancellation invalidates the writer as well, no logging could be performed after that safely.
    * @param fileName name of log file to append to
    * @param formatter formatter to use
    * @param timeWindow pause between flushing log events into file
    * @return [[Logger]] in the context of `F[_]` that will asynchronously write to given file with rate of `timeWindow`
    *        once started
    */
  def fileLogger[F[_]: Concurrent: Timer: ContextShift](
      fileName: String,
      formatter: Formatter = Formatter.default,
      timeWindow: FiniteDuration = 1.second
  ): F[Logger[F]] = {
    AsyncFileLogWriter[F](fileName, timeWindow).map { writer =>
      new FileLogger(writer, formatter)
    }
  }

  /**
    * Unsafe version of [[fileLogger]] that runs uncancellable flush loop in the background during instantiation.
    */
  def fileLoggerUnsafe[F[_]: ConcurrentEffect: Timer: ContextShift](
      fileName: String,
      formatter: Formatter = Formatter.default,
      timeWindow: FiniteDuration = 1.second
  ): Logger[F] = {
    FileLogger(AsyncFileLogWriter.unsafe[F](fileName, timeWindow), formatter)
  }
}
