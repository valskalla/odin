package io.odin

import java.time.LocalDateTime

import cats.Monad
import cats.effect.Timer
import cats.instances.list._
import cats.syntax.all._
import io.odin.loggers.DefaultLogger

import scala.annotation.tailrec

package object config extends FileNamePatternSyntax {

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

  implicit class FileNamePatternInterpolator(private val sc: StringContext) extends AnyVal {

    def file(ps: FileNamePattern*): LocalDateTime => String = {
      StringContext.checkLengths(ps, sc.parts)
      dt => {
        @tailrec
        def rec(args: List[FileNamePattern], parts: List[String], acc: StringBuilder): String = {
          args match {
            case Nil          => acc.append(parts.head).toString()
            case head :: tail => rec(tail, parts.tail, acc.append(parts.head).append(head.extract(dt)))
          }
        }
        rec(ps.toList, sc.parts.toList, new StringBuilder())
      }
    }

  }

  implicit def str2fileNamePattern(str: String): FileNamePattern = {
    Value(str)
  }
}
