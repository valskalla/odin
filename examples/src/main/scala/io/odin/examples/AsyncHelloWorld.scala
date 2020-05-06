package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp, Resource}
import io.odin._
import io.odin.syntax._

/**
  * Async logger runs the internal loop to drain the buffer that accumulates log events.
  *
  * To safely allocate, release and drain this queue, async logger is wrapped in `Resource`
  */
object AsyncHelloWorld extends IOApp {
  val loggerResource: Resource[IO, Logger[IO]] = consoleLogger[IO]().withAsync()

  def run(args: List[String]): IO[ExitCode] =
    loggerResource
      .use(logger => logger.info("Hello world"))
      .as(ExitCode.Success)
}
