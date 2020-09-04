package io.odin.loggers

import cats.Monad
import cats.kernel.laws.{IsEq, _}
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage}

trait LoggerLaws[F[_]] {
  implicit val F: Monad[F]
  val written: F[Unit] => List[LoggerMessage]

  def checksMinLevel(
      logger: Logger[F],
      msg: LoggerMessage,
      level: Level
  ): IsEq[List[LoggerMessage]] = {
    def trace(l: Logger[F]): F[Unit] =
      l.trace(msg.message.value) >> l.trace(msg.message.value, msg.context) >> F.whenA(msg.exception.isDefined) {
        l.trace(msg.message.value, msg.exception.get) >>
          l.trace(msg.message.value, msg.context, msg.exception.get)
      }

    def debug(l: Logger[F]): F[Unit] =
      l.debug(msg.message.value) >> l.debug(msg.message.value, msg.context) >> F.whenA(msg.exception.isDefined) {
        l.debug(msg.message.value, msg.exception.get) >>
          l.debug(msg.message.value, msg.context, msg.exception.get)
      }

    def info(l: Logger[F]): F[Unit] =
      l.info(msg.message.value) >> l.info(msg.message.value, msg.context) >> F.whenA(msg.exception.isDefined) {
        l.info(msg.message.value, msg.exception.get) >>
          l.info(msg.message.value, msg.context, msg.exception.get)
      }

    def warn(l: Logger[F]): F[Unit] =
      l.warn(msg.message.value) >> l.warn(msg.message.value, msg.context) >> F.whenA(msg.exception.isDefined) {
        l.warn(msg.message.value, msg.exception.get) >>
          l.warn(msg.message.value, msg.context, msg.exception.get)
      }

    def error(l: Logger[F]): F[Unit] =
      l.error(msg.message.value) >> l.error(msg.message.value, msg.context) >> F.whenA(msg.exception.isDefined) {
        l.error(msg.message.value, msg.exception.get) >>
          l.error(msg.message.value, msg.context, msg.exception.get)
      }

    def all(l: Logger[F]): F[Unit] =
      trace(l) >> debug(l) >> info(l) >> warn(l) >> error(l)

    written(all(logger.withMinimalLevel(level))) <-> written(all(logger)).filter(msg => msg.level >= level)
  }

  def batchEqualsToTraverse(
      logger: Logger[F],
      msgs: List[LoggerMessage]
  ): IsEq[List[LoggerMessage]] = {
    written(logger.log(msgs)) <-> written(msgs.traverse_(logger.log)(F))
  }
}
