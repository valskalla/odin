package io.odin.slf4j

import cats.effect.{Clock, Effect, IO}
import io.odin.{Level, Logger}

class ExternalLogger extends OdinLoggerBinder[IO] {
  implicit val F: Effect[IO] = IO.ioEffect
  implicit val clock: Clock[IO] = Clock.create

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
