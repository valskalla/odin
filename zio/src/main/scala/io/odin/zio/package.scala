package io.odin

import _root_.zio._
import _root_.zio.clock.Clock
import _root_.zio.interop.CBlocking
import _root_.zio.interop.catz._
import cats.arrow.FunctionK
import cats.effect.std.Dispatcher
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
  )(implicit runtime: Runtime[Clock & CBlocking]): Logger[IO[LoggerError, *]] = {
    io.odin.consoleLogger[Task](formatter, minLevel).mapK(fromTask)
  }

  /**
    * See `io.odin.fileLogger`
    */
  def fileLogger(
      fileName: String,
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): ZManaged[Clock & CBlocking, LoggerError, Logger[IO[LoggerError, *]]] =
    ZManaged
      .fromEffect(ZIO.runtime[Clock & CBlocking].map(rt => asyncRuntimeInstance(rt)))
      .flatMap { implicit F =>
        ZManaged.fromEffect {
          Dispatcher[Task].use { implicit dispatcher =>
            Task(io.odin.fileLogger[Task](fileName, formatter, minLevel).toManaged)
          }
        }
      }
      .flatten
      .mapError(error => LoggerError(error))
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
  ): ZManaged[Clock & CBlocking, LoggerError, Logger[IO[LoggerError, *]]] =
    ZManaged
      .fromEffect(ZIO.runtime[Clock & CBlocking].map(rt => asyncRuntimeInstance(rt)))
      .flatMap { implicit F =>
        ZManaged.fromEffect {
          Dispatcher[Task].use { implicit dispatcher =>
            Task(io.odin.asyncFileLogger[Task](fileName, formatter, timeWindow, maxBufferSize, minLevel).toManaged)
          }
        }
      }
      .flatten
      .mapError(LoggerError.apply)
      .map(_.mapK(fromTask))

  private[odin] val fromTask: Task ~> IO[LoggerError, *] =
    λ[FunctionK[Task, IO[LoggerError, *]]](_.mapError(error => LoggerError(error)))
}
