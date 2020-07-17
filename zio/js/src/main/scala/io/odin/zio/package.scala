package io.odin

import _root_.zio._
import _root_.zio.interop.catz._
import _root_.zio.interop.catz.implicits._
import cats.arrow.FunctionK
import cats.~>
import io.odin.formatter.Formatter

package object zio {

  /**
    * See `io.odin.consoleLogger`
    */
  def consoleLogger(
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Logger[IO[LoggerError, *]] = {
    io.odin.consoleLogger[Task](formatter, minLevel).mapK(fromTask)
  }

  private[odin] val fromTask: Task ~> IO[LoggerError, *] =
    λ[FunctionK[Task, IO[LoggerError, *]]](_.mapError(LoggerError.apply))
}
