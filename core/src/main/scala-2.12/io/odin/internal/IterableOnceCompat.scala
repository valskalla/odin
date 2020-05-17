package io.odin.internal

object IterableOnceCompat {

  type IterableOnceCompat[+A] = TraversableOnce[A]
  implicit class IterableOnceCompatOps[A](val itoc: IterableOnceCompat[A]) extends AnyVal {
    def toSetCompat: Set[A] = itoc.toIterator.toSet
  }

}
