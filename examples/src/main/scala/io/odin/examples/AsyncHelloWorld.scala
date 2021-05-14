package io.odin.examples

import cats.effect.{IO, IOApp, Resource}
import io.odin._
import io.odin.syntax._

/**
  * Async logger runs the internal loop to drain the buffer that accumulates log events.
  *
  * To safely allocate, release and drain this queue, async logger is wrapped in `Resource`
  */
object AsyncHelloWorld extends IOApp.Simple {
  val loggerResource: Resource[IO, Logger[IO]] = consoleLogger[IO]().withAsync()

  def run: IO[Unit] =
    loggerResource
      .use(logger => logger.info("Hello world"))

}
