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

