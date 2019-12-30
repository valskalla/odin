package io.odin

import io.odin.formatter.Formatter
import _root_.monix.eval.Task
import cats.effect.Resource

import scala.concurrent.duration._

package object monix {

  /**
    * See `io.odin.consoleLogger`
    */
  def consoleLogger(
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Logger[Task] =
    io.odin.consoleLogger[Task](formatter, minLevel)

  /**
    * See `io.odin.fileLogger`
    */
  def fileLogger(
      fileName: String,
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Resource[Task, Logger[Task]] =
    io.odin.fileLogger[Task](fileName, formatter, minLevel)

  /**
    * See `io.odin.asyncFileLogger`
    */
  def asyncFileLogger(
      fileName: String,
      formatter: Formatter = Formatter.default,
      timeWindow: FiniteDuration = 1.second,
      maxBufferSize: Option[Int] = None,
      minLevel: Level = Level.Trace
  ): Resource[Task, Logger[Task]] =
    io.odin.asyncFileLogger[Task](fileName, formatter, timeWindow, maxBufferSize, minLevel)
}
