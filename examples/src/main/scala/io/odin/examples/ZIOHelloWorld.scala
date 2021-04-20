package io.odin.examples

import io.odin.Logger
import zio._
import io.odin.zio._

object ZIOHelloWorld extends App {
  val logger: Logger[IO[LoggerError, *]] = consoleLogger()(this)

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    logger.info("Hello world").fold(_ => ExitCode.failure, _ => ExitCode.success)
}
