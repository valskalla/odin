package io.odin.internal

object StringContextLength {

  def checkLength(sc: StringContext, args: Seq[Any]): Unit =
    sc.checkLengths(args)

}
