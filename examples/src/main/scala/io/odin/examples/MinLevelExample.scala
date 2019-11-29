package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._
import io.odin.Logger
import io.odin._
import io.odin.syntax._

/**
  * Only warning message will be printed to the stderr, since this logger defines minimal level
  */
object MinLevelExample extends IOApp {
  val logger: Logger[IO] = consoleLogger[IO]().withMinimalLevel(Level.Warn)

  def run(args: List[String]): IO[ExitCode] = {
    (logger.info("Hello?") *> logger.warn("Hi there")).as(ExitCode.Success)
  }
}
