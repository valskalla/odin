package io.odin.examples

import cats.effect.{IO, IOApp}
import io.odin._
import io.odin.formatter.Formatter

/**
  * Prints simple `Hello World` log line
  */
object HelloWorld extends IOApp.Simple {
  val logger: Logger[IO] = consoleLogger(formatter = Formatter.colorful)

  def run: IO[Unit] =
    logger.info("Hello world")
}
