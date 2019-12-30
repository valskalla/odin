package io.odin.examples

import cats.data.ReaderT
import cats.effect.{ExitCode, IO, IOApp}
import io.odin._
import cats.mtl.instances.all._
import cats.syntax.all._
import io.odin.loggers.HasContext
import io.odin.syntax._

/**
  * Prints `Hello World` log line with some context picked up from the environment of `F[_]`
  *
  * In cases when there is `ApplicativeAsk[F, Env]` available in scope, and there is type class [[HasContext]] for
  * environment `Env` defined, `withContext` will automatically derive required type classes for adding the context to
  * the log
  */
object WithContextual extends IOApp {

  /**
    * Simple Reader monad with environment being context `Map[String, String]`
    */
  type F[A] = ReaderT[IO, Map[String, String], A]

  /**
    * Define how to pick the context out of [[F]] environment. Here it's just identity
    */
  implicit val hasContext: HasContext[Map[String, String]] = new HasContext[Map[String, String]] {
    def getContext(env: Map[String, String]): Map[String, String] = env
  }

  /**
    * `withContext` requires `WithContext[F]` type class but with `import cats.mtl.instances.all._` that provides
    * `ApplicativeAsk[F, Env]` and corresponding `HasContext` instance defined, this type class is derived automatically
    */
  val logger: Logger[F] = consoleLogger[F]().withContext

  def run(args: List[String]): IO[ExitCode] =
    logger.info("Hello world").run(Map("this is" -> "context")).as(ExitCode.Success)
}
