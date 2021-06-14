package io.odin.examples

import cats.data.ReaderT
import cats.effect.{IO, IOApp}
import io.odin._
import io.odin.formatter.Formatter
import io.odin.loggers.HasContext
import io.odin.syntax._

/**
  * Prints `Hello World` log line with some context picked up from the environment of `F[_]`
  *
  * In cases when there is `Ask[F, Env]` available in scope, and there is type class `io.odin.loggers.HasContext` for
  * environment `Env` defined, `withContext` will automatically derive required type classes for adding the context to
  * the log
  */
object WithContextual extends IOApp.Simple {

  /**
    * Simple Reader monad with environment being context `Map[String, String]`
    */
  type F[A] = ReaderT[IO, Map[String, String], A]

  /**
    * Define how to pick the context out of [[F]] environment. Here it's just identity
    */
  implicit val hasContext: HasContext[Map[String, String]] = (env: Map[String, String]) => env

  /**
    * `withContext` requires `WithContext[F]` type class but with the corresponding `HasContext` instance defined,
    * this type class is derived automatically
    */
  val logger: Logger[F] = consoleLogger[F](formatter = Formatter.colorful).withContext

  def run: IO[Unit] =
    logger.info("Hello world").run(Map("this is" -> "context"))
}
