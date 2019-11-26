package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._
import io.odin._
import io.odin.syntax._

/**
  * Prints `Hello World` log line with some predefined constant context
  */
object WithConstContext extends IOApp {
  val logger: Logger[IO] = consoleLogger[IO]().withConstContext(Map("this is" -> "context"))

  def run(args: List[String]): IO[ExitCode] =
    logger.info("Hello world").as(ExitCode.Success)
}
