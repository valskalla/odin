package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp}
import io.odin.{Logger, _}

/**
  * Only warning message will be printed to the STDERR since this logger defines minimal level
  */
object MinLevelExample extends IOApp {
  val logger: Logger[IO] = consoleLogger[IO]().withMinimalLevel(Level.Warn)

  def run(args: List[String]): IO[ExitCode] = {
    (logger.info("Hello?") *> logger.warn("Hi there")).as(ExitCode.Success)
  }
}
