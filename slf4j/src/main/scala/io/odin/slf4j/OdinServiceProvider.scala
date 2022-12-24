package io.odin.slf4j
import org.slf4j.spi.SLF4JServiceProvider;
import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.helpers.{BasicMarkerFactory, NOPMDCAdapter}
import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import io.odin.Logger

abstract class OdinServiceProvider[F[_]] extends SLF4JServiceProvider {

  implicit def F: Sync[F]
  implicit def dispatcher: Dispatcher[F]

  def loggers: PartialFunction[String, Logger[F]]

  private val loggerFactory = new OdinLoggerFactory[F](loggers)
  private val mdcAdapter = new NOPMDCAdapter()
  private val markerFactory = new BasicMarkerFactory

  override def getLoggerFactory(): ILoggerFactory = loggerFactory

  override def getMarkerFactory(): IMarkerFactory = markerFactory

  override def getMDCAdapter(): MDCAdapter = mdcAdapter

  override def getRequestedApiVersion(): String = "2.0.0"

  override def initialize(): Unit = ()
}
