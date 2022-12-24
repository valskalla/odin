package io.odin.slf4j

import cats.effect.IO
import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import io.odin.{Level, Logger}

class ExternalLoggerProvider extends OdinServiceProvider[IO] {
  implicit def F: Sync[IO] = IO.asyncForIO
  implicit def dispatcher: Dispatcher[IO] = Dispatcher[IO].allocated.unsafeRunSync()._1

  def loggers: PartialFunction[String, Logger[IO]] = {
    case Level.Trace.toString => new BufferingLogger[IO](Level.Trace)
    case Level.Debug.toString => new BufferingLogger[IO](Level.Debug)
    case Level.Info.toString  => new BufferingLogger[IO](Level.Info)
    case Level.Warn.toString  => new BufferingLogger[IO](Level.Warn)
    case Level.Error.toString => new BufferingLogger[IO](Level.Error)
    case _ =>
      new BufferingLogger[IO](Level.Trace)
  }
}
