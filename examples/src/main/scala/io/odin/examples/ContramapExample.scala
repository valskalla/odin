package io.odin.examples

import cats.effect.{IO, IOApp}
import cats.syntax.all._
import io.odin._
import io.odin.syntax._

/**
  * Modify logger message before it's written
  */
object ContramapExample extends IOApp.Simple {

  /**
    * This logger always appends " World" string to each message
    */
  def logger: Logger[IO] = consoleLogger[IO]().contramap(msg => msg.copy(message = msg.message.map(_ + " World")))

  def run: IO[Unit] =
    logger.info("Hello")
}
