package io.odin.loggers

import cats.Monad
import cats.effect.Clock
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage}

case class RouterLogger[F[_]: Clock: Monad](router: PartialFunction[LoggerMessage, Logger[F]])
    extends DefaultLogger[F] {

  private val noop = Logger.noop
  private val withFallback: PartialFunction[LoggerMessage, Logger[F]] = router.orElse { case _ => noop }

  def log(msg: LoggerMessage): F[Unit] = withFallback(msg).log(msg)
}

object RouterLogger extends RouterLoggerBuilder

trait RouterLoggerBuilder {

  /**
    * Route logs to specific logger based on the fully qualified package name.
    * Beware of O(n) complexity due to the partial matching done during the logging
    */
  def packageRoutingLogger[F[_]: Clock: Monad](router: (String, Logger[F])*): Logger[F] = {
    val noop = Logger.noop
    RouterLogger {
      case msg =>
        router
          .collectFirst {
            case (packageName, logger) if msg.position.packageName.startsWith(packageName) =>
              logger
          }
          .getOrElse(noop)
    }
  }

  /**
    * Route logs to specific logger based on `Class[_]` instance.
    * Beware of O(n) complexity due to the partial matching done during the logging
    */
  def classRoutingLogger[F[_]: Clock: Monad](router: (Class[_], Logger[F])*): Logger[F] = {
    val noop = Logger.noop
    val classNameRouter = router.map {
      case (cls, logger) => (cls.getName, logger)
    }
    RouterLogger {
      case msg =>
        classNameRouter
          .collectFirst {
            case (className, logger) if msg.position.enclosureName.startsWith(className) =>
              logger
          }
          .getOrElse(noop)
    }
  }

  /**
    * Route logs based on their level
    *
    * Complexity should be roughly constant and based on Map hashing/collisions
    */
  def levelRoutingLogger[F[_]: Clock: Monad](router: Map[Level, Logger[F]]): Logger[F] =
    RouterLogger {
      case msg if router.contains(msg.level) => router(msg.level)
    }

  /**
    * Route logs only if the level is greater or equal to the defined level. Otherwise logs are nooped.
    */
  def withMinimalLevel[F[_]: Clock: Monad](level: Level)(inner: Logger[F]): Logger[F] =
    RouterLogger {
      case msg if msg.level >= level => inner
    }

}
