package io.odin.examples

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._
import io.odin._
import io.odin.config._

/**
  * Only one logger will print the message, as one of them will be routed to the logger with minimal level WARN,
  * but both print only info messages
  */
object ClassBasedRouting extends IOApp {
  val logger: Logger[IO] =
    classRouting[IO](
      classOf[Foo[_]] -> consoleLogger[IO]().withMinimalLevel(Level.Warn),
      classOf[Bar[_]] -> consoleLogger[IO]().withMinimalLevel(Level.Info)
    ).withNoopFallback

  def run(args: List[String]): IO[ExitCode] = {
    (Foo(logger).log *> Bar(logger).log).as(ExitCode.Success)
  }
}

case class Foo[F[_]](logger: Logger[F]) {
  def log: F[Unit] = logger.info("foo")
}

case class Bar[F[_]](logger: Logger[F]) {
  def log: F[Unit] = logger.info("bar")
}
