package io.odin.formatter.options

final case class ThrowableFormat(depth: ThrowableFormat.Depth, indent: ThrowableFormat.Indent)

object ThrowableFormat {

  val Default: ThrowableFormat = ThrowableFormat(Depth.Full, Indent.NoIndent)

  sealed trait Depth
  object Depth {
    final case object Full extends Depth
    final case class Fixed(size: Int) extends Depth
  }

  sealed trait Indent
  object Indent {
    final case object NoIndent extends Indent
    final case class Fixed(size: Int) extends Indent
  }

}
