package io.odin.loggers

import cats.Monad
import cats.effect.kernel.Clock
import cats.mtl.Ask
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Logger that extracts context from environment of `F[_]` with the help of [[WithContext]] type class.
  *
  * One of the examples of `F[_]` that has a context is `Reader` (also known as `Kleisli`) that is abstraction over
  * function `A => M[B]`. If there is a way to extract context `Map[String, String]` from the `A` (see [[HasContext]]),
  * then it's possible to add this context to the log.
  */
case class ContextualLogger[F[_]: Clock: Monad](inner: Logger[F])(implicit withContext: WithContext[F])
    extends DefaultLogger[F](inner.minLevel) {
  def submit(msg: LoggerMessage): F[Unit] =
    withContext.context.flatMap { ctx =>
      inner.log(msg.copy(context = msg.context ++ ctx))
    }

  override def submit(msgs: List[LoggerMessage]): F[Unit] =
    withContext.context.flatMap { ctx =>
      inner.log(msgs.map(msg => msg.copy(context = msg.context ++ ctx)))
    }

  def withMinimalLevel(level: Level): Logger[F] = copy(inner = inner.withMinimalLevel(level))
}

object ContextualLogger {
  def withContext[F[_]: Clock: Monad: WithContext](inner: Logger[F]): Logger[F] = ContextualLogger(inner)
}

/**
  * Extract log context from environment
  */
trait HasContext[Env] {
  def getContext(env: Env): Map[String, String]
}

/**
  * Resolve context stored in `F[_]` effect
  */
trait WithContext[F[_]] {
  def context: F[Map[String, String]]
}

object WithContext {

  /**
    * Default implementation of `WithContext` that works for any `F[_]` with `Ask` instance and
    * instance of [[HasContext]] for environment of this `Ask`
    */
  implicit def fromHasContext[F[_], Env](
      implicit A: Ask[F, Env],
      hasContext: HasContext[Env],
      F: Monad[F]
  ): WithContext[F] = new WithContext[F] {
    def context: F[Map[String, String]] = A.ask.map(hasContext.getContext)
  }
}
