package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp}
import io.odin._
import cats.syntax.all._
import io.odin.formatter.Formatter

/**
  * Prints simple `Hello World` log line
  */
object HelloWorld extends IOApp {
  val logger: Logger[IO] = consoleLogger(formatter = Formatter.colorful)

  def run(args: List[String]): IO[ExitCode] =
    logger.info("Hello world").as(ExitCode.Success)
}
