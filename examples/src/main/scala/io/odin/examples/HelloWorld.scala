package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp}
import io.odin._
import cats.syntax.all._

/**
  * Prints simple `Hello World` log line
  */
object HelloWorld extends IOApp {

  val logger: Logger[IO] = consoleLogger()

  def run(args: List[String]): IO[ExitCode] =
    logger.info("Hello world").as(ExitCode.Success)
}
