package io.odin

import _root_.zio._
import _root_.zio.interop.catz._
import _root_.zio.interop.catz.implicits._
import cats.arrow.FunctionK
import cats.~>
import io.odin.formatter.Formatter

import scala.concurrent.duration._

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

  /**
    * See `io.odin.fileLogger`
    */
  def fileLogger(
      fileName: String,
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Managed[LoggerError, Logger[IO[LoggerError, *]]] =
    ZManaged
      .fromEffect(Task.concurrentEffect)
      .flatMap { implicit F =>
        io.odin.fileLogger[Task](fileName, formatter, minLevel).toManaged
      }
      .mapError(LoggerError.apply)
      .map(_.mapK(fromTask))

  /**
    * See `io.odin.asyncFileLogger`
    */
  def asyncFileLogger(
      fileName: String,
      formatter: Formatter = Formatter.default,
      timeWindow: FiniteDuration = 1.second,
      maxBufferSize: Option[Int] = None,
      minLevel: Level = Level.Trace
  ): Managed[LoggerError, Logger[IO[LoggerError, *]]] =
    ZManaged
      .fromEffect(Task.concurrentEffect)
      .flatMap { implicit F =>
        io.odin.asyncFileLogger[Task](fileName, formatter, timeWindow, maxBufferSize, minLevel).toManaged
      }
      .mapError(LoggerError.apply)
      .map(_.mapK(fromTask))

  private[odin] val fromTask: Task ~> IO[LoggerError, *] =
    λ[FunctionK[Task, IO[LoggerError, *]]](_.mapError(LoggerError.apply))
}
