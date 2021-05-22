package io.odin.formatter.options

final case class ThrowableFormat(
    depth: ThrowableFormat.Depth,
    indent: ThrowableFormat.Indent,
    filter: ThrowableFormat.Filter
)

object ThrowableFormat {

  val Default: ThrowableFormat = ThrowableFormat(Depth.Full, Indent.NoIndent, Filter.NoFilter)

  sealed trait Depth
  object Depth {
    case object Full extends Depth
    case class Fixed(size: Int) extends Depth
  }

  sealed trait Indent
  object Indent {
    case object NoIndent extends Indent
    case class Fixed(size: Int) extends Indent
  }

  sealed trait Filter
  object Filter {
    case object NoFilter extends Filter
    case class Excluding(prefixes: Set[String]) extends Filter

    object Excluding {
      def apply(prefixes: String*): Excluding = new Excluding(prefixes.toSet)
    }
  }

}
