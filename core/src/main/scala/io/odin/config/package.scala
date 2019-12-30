package io.odin

import cats.Monad
import cats.effect.Timer
import cats.instances.list._
import cats.syntax.all._
import io.odin.loggers.DefaultLogger

package object config {

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
  def classRouting[F[_]: Timer: Monad](
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
  def levelRouting[F[_]: Timer: Monad](router: Map[Level, Logger[F]]): DefaultBuilder[F] =
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
}
