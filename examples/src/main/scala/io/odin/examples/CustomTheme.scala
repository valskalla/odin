package io.odin.examples

import cats.effect.{IO, IOApp}
import io.odin._
import io.odin.formatter.{Formatter, Theme}
import scala.io.AnsiColor._

/**
  * Prints simple `Hello World` log line using a custom colorful theme
  */
object CustomTheme extends IOApp.Simple {
  val theme = Theme.ansi.copy(
    level = CYAN,
    threadName = YELLOW,
    timestamp = RED
  )

  val logger: Logger[IO] = consoleLogger(formatter = Formatter.create(theme))

  def run: IO[Unit] =
    logger.info("Hello world")
}
