package io.odin.loggers

import cats.Monad
import cats.effect.Timer
import cats.mtl.ApplicativeAsk
import cats.syntax.all._
import io.odin.{Logger, LoggerMessage}

/**
  * Logger that extracts context from environment of `F[_]` with the help of [[WithContext]] type class.
  *
  * One of the examples of `F[_]` that has a context is `Reader` (also known as `Kleisli`) that is abstraction over
  * function `A => M[B]`. If there is a way to extract context `Map[String, String]` from the `A` (see [[HasContext]]),
  * then it's possible to add this context to the log.
  */
case class ContextualLogger[F[_]: Timer: Monad](inner: Logger[F])(implicit withContext: WithContext[F])
    extends DefaultLogger[F] {
  def log(msg: LoggerMessage): F[Unit] =
    withContext.context.flatMap { ctx =>
      inner.log(msg.copy(context = msg.context ++ ctx))
    }

  override def log(msgs: List[LoggerMessage]): F[Unit] =
    withContext.context.flatMap { ctx =>
      inner.log(msgs.map(msg => msg.copy(context = msg.context ++ ctx)))
    }
}

object ContextualLogger {
  def withContext[F[_]: Timer: Monad: WithContext](inner: Logger[F]): Logger[F] = ContextualLogger(inner)
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
    * Default implementation of `WithContext` that works for any `F[_]` with `ApplicativeAsk` instance and
    * instance of [[HasContext]] for environment of this `ApplicativeAsk`
    */
  implicit def fromHasContext[F[_], Env](
      implicit A: ApplicativeAsk[F, Env],
      hasContext: HasContext[Env],
      F: Monad[F]
  ): WithContext[F] = new WithContext[F] {
    def context: F[Map[String, String]] = A.ask.map(hasContext.getContext)
  }
}
