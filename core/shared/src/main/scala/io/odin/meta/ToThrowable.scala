package io.odin.meta

/**
  * Type class that converts a value of type `E` into Throwable
  */
trait ToThrowable[E] {
  def throwable(e: E): Throwable
}

object ToThrowable {
  implicit def toThrowable[E <: Throwable]: ToThrowable[E] = (e: E) => e
}
