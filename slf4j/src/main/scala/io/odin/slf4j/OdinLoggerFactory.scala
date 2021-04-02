package io.odin.slf4j

import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import io.odin.{Logger => OdinLogger}
import org.slf4j.{ILoggerFactory, Logger}

class OdinLoggerFactory[F[_]: Sync: Dispatcher](loggers: PartialFunction[String, OdinLogger[F]]) extends ILoggerFactory {
  def getLogger(name: String): Logger = {
    new OdinLoggerAdapter[F](name, loggers.applyOrElse(name, (_: String) => OdinLogger.noop))
  }
}
