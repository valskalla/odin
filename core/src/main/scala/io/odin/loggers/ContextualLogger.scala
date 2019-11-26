package io.odin.loggers

import cats.Monad
import cats.effect.Clock
import cats.mtl.ApplicativeAsk
import cats.syntax.all._
import io.odin.{Logger, LoggerMessage}

/**
  * Logger that extracts context from environment of `F[_]` with the help of [[WithContext]] type class
  */
case class ContextualLogger[F[_]: Clock: Monad](inner: Logger[F])(implicit withContext: WithContext[F])
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
