package io.odin.slf4j

import cats.effect.{Clock, Effect}
import io.odin.{Logger => OdinLogger}
import org.slf4j.{ILoggerFactory, Logger}

class OdinLoggerFactory[F[_]: Effect: Clock](loggers: PartialFunction[String, OdinLogger[F]]) extends ILoggerFactory {
  def getLogger(name: String): Logger = {
    new OdinLoggerAdapter[F](name, loggers.applyOrElse(name, (_: String) => OdinLogger.noop))
  }
}
