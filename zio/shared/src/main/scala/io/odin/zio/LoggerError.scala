package io.odin.zio

import scala.util.control.{NoStackTrace, NonFatal}

/**
  * Possible errors raised by logger or during resources allocation
  */
sealed trait LoggerError extends Throwable with NoStackTrace {
  def inner: Throwable
}

case class IOException(inner: java.io.IOException) extends LoggerError

case class SecurityException(inner: java.lang.SecurityException) extends LoggerError

case class Unknown(inner: Throwable) extends LoggerError

object LoggerError {
  def apply(t: Throwable): LoggerError = t match {
    case io: java.io.IOException          => IOException(io)
    case sec: java.lang.SecurityException => SecurityException(sec)
    case NonFatal(t)                      => Unknown(t)
  }
}
