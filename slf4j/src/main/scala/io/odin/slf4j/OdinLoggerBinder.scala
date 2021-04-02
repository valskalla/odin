package io.odin.slf4j

import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import io.odin.Logger
import org.slf4j.ILoggerFactory
import org.slf4j.spi.LoggerFactoryBinder

abstract class OdinLoggerBinder[F[_]] extends LoggerFactoryBinder {

  implicit def F: Sync[F]
  implicit def dispatcher: Dispatcher[F]

  def loggers: PartialFunction[String, Logger[F]]

  private val factoryClass = classOf[OdinLoggerFactory[F]].getName

  override def getLoggerFactory: ILoggerFactory = new OdinLoggerFactory[F](loggers)

  override def getLoggerFactoryClassStr: String = factoryClass

}
