package io.odin.slf4j

import cats.effect.{ConcurrentEffect, Timer}
import io.odin.Logger
import org.slf4j.ILoggerFactory
import org.slf4j.spi.LoggerFactoryBinder

abstract class OdinLoggerBinder[F[_]] extends LoggerFactoryBinder {

  implicit def timer: Timer[F]
  implicit def F: ConcurrentEffect[F]

  def loggers: PartialFunction[String, Logger[F]]

  private val factoryClass = classOf[OdinLoggerFactory[F]].getName

  override def getLoggerFactory: ILoggerFactory = new OdinLoggerFactory[F](loggers)

  override def getLoggerFactoryClassStr: String = factoryClass

}
