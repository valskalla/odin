package io.odin.examples

import cats.effect.{IO, IOApp}
import io.odin._
import io.odin.syntax._

/**
  * Prints `Hello World` log line with some predefined constant context
  */
object WithConstContext extends IOApp.Simple {
  val logger: Logger[IO] = consoleLogger[IO]().withConstContext(Map("this is" -> "context"))

  def run: IO[Unit] =
    logger.info("Hello world")
}
