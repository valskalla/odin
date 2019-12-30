package org.slf4j.impl

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import io.odin._
import io.odin.slf4j.{BufferingLogger, OdinLoggerBinder}

import scala.concurrent.ExecutionContext

class StaticLoggerBinder extends OdinLoggerBinder[IO] {

  val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  val loggers: PartialFunction[String, Logger[IO]] = {
    case Level.Trace.toString => new BufferingLogger[IO](Level.Trace)
    case Level.Debug.toString => new BufferingLogger[IO](Level.Debug)
    case Level.Info.toString  => new BufferingLogger[IO](Level.Info)
    case Level.Warn.toString  => new BufferingLogger[IO](Level.Warn)
    case Level.Error.toString => new BufferingLogger[IO](Level.Error)
    case _ =>
      new BufferingLogger[IO](Level.Trace)
  }
}

object StaticLoggerBinder extends StaticLoggerBinder {

  var REQUESTED_API_VERSION: String = "1.7"

  def getSingleton: StaticLoggerBinder = this

}
