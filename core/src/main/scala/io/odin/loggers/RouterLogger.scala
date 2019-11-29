package io.odin.loggers

import cats.Monad
import cats.effect.Timer
import cats.instances.list._
import cats.instances.map._
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage}

import scala.annotation.tailrec

object RouterLogger {
  /**
    * Route logs to specific logger based on the fully qualified package name.
    * Beware of O(n) complexity due to the partial matching done during the logging
    */
  def enclosureRouting[F[_]: Timer: Monad](router: (String, Logger[F])*): DefaultBuilder[F] = {
    new DefaultBuilder[F](new EnclosureRouting(_, router.toList))
  }

  /**
    * Route logs to specific logger based on `Class[_]` instance.
    * Beware of O(n) complexity due to the partial matching done during the logging
    */
  def classRoutingLogger[F[_]: Timer: Monad](
      router: (Class[_], Logger[F])*
  ): DefaultBuilder[F] =
    new DefaultBuilder[F](new EnclosureRouting(_, router.toList.map {
      case (cls, logger) => cls.getName -> logger
    }))

  /**
    * Route logs based on their level
    *
    * Complexity should be roughly constant
    */
  def levelRoutingLogger[F[_]: Timer: Monad](router: Map[Level, Logger[F]]): DefaultBuilder[F] =
    new DefaultBuilder[F]({ default: Logger[F] =>
      new DefaultLogger[F]() {
        def log(msg: LoggerMessage): F[Unit] = router.getOrElse(msg.level, default).log(msg)

        override def log(msgs: List[LoggerMessage]): F[Unit] = {
          msgs.groupBy(_.level).toList.traverse_ {
            case (level, msgs) => router.getOrElse(level, default).log(msgs)
          }
        }
      }
    })

  /**
    * Route logs only if the level is greater or equal to the defined level. Otherwise logs are nooped.
    */
  def withMinimalLevel[F[_]: Timer](level: Level, inner: Logger[F])(implicit F: Monad[F]): Logger[F] =
    new DefaultLogger[F]() {
      def log(msg: LoggerMessage): F[Unit] = if (msg.level >= level) inner.log(msg) else F.unit
      override def log(msgs: List[LoggerMessage]): F[Unit] = {
        msgs.filter(_.level >= level) match {
          case Nil  => F.unit
          case list => inner.log(list)
        }
      }
    }

  private class EnclosureRouting[F[_]: Monad: Timer](fallback: Logger[F], router: List[(String, Logger[F])])
      extends DefaultLogger {
    private val indexedRouter = router.mapWithIndex {
      case ((packageName, logger), idx) => (packageName, (idx, logger))
    }

    def log(msg: LoggerMessage): F[Unit] = recLog(indexedRouter, msg)

    override def log(msgs: List[LoggerMessage]): F[Unit] = {
      msgs
        .map { msg =>
          indexedRouter
            .collectFirst {
              case (key, indexedLogger) if msg.position.enclosureName.startsWith(key) => indexedLogger
            }
            .getOrElse(-1 -> fallback) -> List(msg)
        }
        .foldLeft(Map.empty[(Int, Logger[F]), List[LoggerMessage]]) {
          case (map, kv) => map |+| Map(kv)
        }
        .toList
        .traverse_ {
          case ((_, logger), ms) => logger.log(ms)
        }
    }

    @tailrec
    private def recLog(router: List[(String, (Int, Logger[F]))], msg: LoggerMessage): F[Unit] = router match {
      case Nil                                                                   => fallback.log(msg)
      case (key, (_, logger)) :: _ if msg.position.enclosureName.startsWith(key) => logger.log(msg)
      case _ :: tail                                                             => recLog(tail, msg)
    }
  }

  class DefaultBuilder[F[_]: Timer: Monad](withDefault: Logger[F] => Logger[F]) {
    def withNoopFallback: Logger[F] =
      withDefault(Logger.noop[F])
    def withFallback(fallback: Logger[F]): Logger[F] =
      withDefault(fallback)
  }
}
