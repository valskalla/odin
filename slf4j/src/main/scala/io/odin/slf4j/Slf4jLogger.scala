package io.odin.slf4j

import cats.effect.kernel.Sync
import io.odin.formatter.Formatter
import io.odin.loggers.DefaultLogger
import io.odin.{Level, Logger, LoggerMessage}
import org.slf4j.{Logger => JLogger, LoggerFactory}
import cats.implicits._

final class Slf4jLogger[F[_]: Sync](logger: JLogger, level: Level, formatter: Formatter)
    extends DefaultLogger[F](level) {
  override def submit(msg: LoggerMessage): F[Unit] = {
    Sync[F].uncancelable { _ =>
      Sync[F].whenA(msg.level >= this.minLevel)(msg.level match {
        case Level.Trace => Sync[F].delay(logger.trace(formatter.format(msg)))
        case Level.Debug => Sync[F].delay(logger.debug(formatter.format(msg)))
        case Level.Info  => Sync[F].delay(logger.info(formatter.format(msg)))
        case Level.Warn  => Sync[F].delay(logger.warn(formatter.format(msg)))
        case Level.Error => Sync[F].delay(logger.error(formatter.format(msg)))
      })
    }
  }

  override def withMinimalLevel(level: Level): Logger[F] = new Slf4jLogger[F](logger, level, formatter)
}

object Slf4jLogger {
  def default[F[_]: Sync]: Slf4jLogger[F] =
    new Slf4jLogger[F](LoggerFactory.getLogger("OdinSlf4jLogger"), Level.Info, Formatter.default)
  def defaultFromSlf4j[F[_]: Sync](logger: JLogger): Slf4jLogger[F] =
    new Slf4jLogger[F](logger, Level.Info, Formatter.default)
}
