package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._
import io.odin._
import io.odin.config._

/**
  * Routing based on the enclosure, would it be a package, object, class or the function.
  *
  * Mind that match is done in order of definition, therefore the most specific routes should always appear on top
  */
object EnclosureBasedRouting extends IOApp {
  val logger: Logger[IO] =
    enclosureRouting(
      "io.odin.examples.EnclosureBasedRouting.foo" -> consoleLogger[IO]().withMinimalLevel(Level.Warn),
      "io.odin.examples.EnclosureBasedRouting.bar" -> consoleLogger[IO]().withMinimalLevel(Level.Info),
      "io.odin.examples" -> consoleLogger[IO]()
    )
    .withNoopFallback

  def zoo: IO[Unit] = logger.debug("Debug")
  def foo: IO[Unit] = logger.info("Never shown")
  def bar: IO[Unit] = logger.warn("Warning")

  def run(args: List[String]): IO[ExitCode] = {
    (zoo *> foo *> bar).as(ExitCode.Success)
  }
}
