package io.odin.examples

import io.odin.Logger
import zio._
import io.odin.zio._

object ZIOHelloWorld extends App {
  val logger: Logger[IO[LoggerError, *]] = consoleLogger()

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    logger.info("Hello world").fold(_ => 1, _ => 0)
}
