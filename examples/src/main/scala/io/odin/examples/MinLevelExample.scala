package io.odin.examples

import cats.effect.{IO, IOApp}
import io.odin.{Logger, _}

/**
  * Only warning message will be printed to the STDERR since this logger defines minimal level
  */
object MinLevelExample extends IOApp.Simple {
  val logger: Logger[IO] = consoleLogger[IO]().withMinimalLevel(Level.Warn)

  def run: IO[Unit] = {
    (logger.info("Hello?") *> logger.warn("Hi there"))
  }
}
