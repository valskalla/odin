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
    case _ =>
      new BufferingLogger[IO]()
  }
}

object StaticLoggerBinder extends StaticLoggerBinder {

  var REQUESTED_API_VERSION: String = "1.7"

  def getSingleton: StaticLoggerBinder = this

}
