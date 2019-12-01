package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._
import io.odin._
import io.odin.syntax._

/**
  * Modify logger message before it's written
  */
object ContramapExample extends IOApp {
  /**
    * This logger always appends " World" string to each message
    */
  def logger: Logger[IO] = consoleLogger[IO]().contramap(msg => msg.copy(message = msg.message.map(_ + " World")))

  def run(args: List[String]): IO[ExitCode] =
    logger.info("Hello").as(ExitCode.Success)
}
