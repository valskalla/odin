package io.odin.loggers

import cats.Monad
import cats.effect.Clock
import cats.mtl.ApplicativeAsk
import cats.syntax.all._
import io.odin.{Logger, LoggerMessage}

/**
  * Logger that extracts context from environment of `F[_]` with the help of [[WithContext]] type class
  */
class ContextualLogger[F[_]: Clock](inner: Logger[F])(implicit F: Monad[F], withContext: WithContext[F])
    extends DefaultLogger[F] {
  def log(msg: LoggerMessage): F[Unit] =
    for {
      ctx <- withContext.context
      _ <- inner.log(msg.copy(context = msg.context ++ ctx))
    } yield {
      ()
    }
}

/**
  * Extract log context from environment in `F[_]` scope
  */
trait HasContext[F[_], Env] {

  def getContext(env: Env): F[Map[String, String]]

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
      hasContext: HasContext[F, Env],
      F: Monad[F]
  ): WithContext[F] = new WithContext[F] {
    def context: F[Map[String, String]] = A.ask.map(hasContext.getContext)
  }

}
