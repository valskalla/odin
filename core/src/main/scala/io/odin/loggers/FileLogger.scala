package io.odin.loggers

import cats.Monad
import cats.effect.{Clock, Concurrent, ConcurrentEffect, ContextShift, Timer}
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.writers.{AsyncFileLogWriter, LogWriter}
import io.odin.{Logger, LoggerMessage}

import scala.concurrent.duration._

/**
  * Write to given log writer with provided formatter
  */
case class FileLogger[F[_]: Clock: Monad](logWriter: LogWriter[F], formatter: Formatter) extends DefaultLogger[F] {
  def log(msg: LoggerMessage): F[Unit] = logWriter.write(msg, formatter)
}

object FileLogger {

  /**
    * Safely start async file logger. Cancellation of `F[_]` will be propagated down the chain to safely close the buffer.
    * BEWARE that cancellation invalidates the writer as well, no logging could be performed after that safely.
    * @param fileName name of log file to append to
    * @param formatter formatter to use
    * @param timeWindow pause between flushing log events into file
    * @return [[Logger]] in the context of `F[_]` that will asynchronously write to given file with rate of `timeWindow`
    *        once started
    */
  def apply[F[_]: Concurrent: Timer: ContextShift](
      fileName: String,
      formatter: Formatter,
      timeWindow: FiniteDuration = 1.second
  ): F[Logger[F]] = {
    AsyncFileLogWriter[F](fileName, timeWindow).map { writer =>
      new FileLogger(writer, formatter)
    }
  }

  def unsafe[F[_]: ConcurrentEffect: Timer: ContextShift](
      fileName: String,
      formatter: Formatter,
      timeWindow: FiniteDuration = 1.second
  ): Logger[F] = {
    FileLogger(AsyncFileLogWriter.unsafe[F](fileName, timeWindow), formatter)
  }

}
