package io.odin.internal

object IterableOnceCompat {

  type IterableOnceCompat[+A] = IterableOnce[A]
  implicit class IterableOnceCompatOps[A](val itoc: IterableOnceCompat[A]) extends AnyVal {
    def toSetCompat: Set[A] = itoc.iterator.toSet
  }

}
