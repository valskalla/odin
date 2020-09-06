package io.odin.config

import cats.Monad
import cats.effect.Timer
import cats.syntax.all._
import io.odin.loggers.DefaultLogger
import io.odin.{Logger, LoggerMessage}

import scala.annotation.tailrec

private[config] class EnclosureRouting[F[_]: Timer](fallback: Logger[F], router: List[(String, Logger[F])])(
    implicit F: Monad[F]
) extends DefaultLogger {
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
        case ((_, logger), ms) => logger.log(ms.filter(_.level >= logger.minLevel))
      }
  }

  @tailrec
  private def recLog(router: List[(String, (Int, Logger[F]))], msg: LoggerMessage): F[Unit] = router match {
    case Nil =>
      if (msg.level >= fallback.minLevel) fallback.log(msg)
      else F.unit
    case (key, (_, logger)) :: _ if msg.position.enclosureName.startsWith(key) && msg.level >= logger.minLevel =>
      logger.log(msg)
    case _ :: tail => recLog(tail, msg)
  }
}
