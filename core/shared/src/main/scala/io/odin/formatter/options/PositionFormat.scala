package io.odin.formatter.options

sealed trait PositionFormat

object PositionFormat {
  final case object Full extends PositionFormat
  final case object AbbreviatePackage extends PositionFormat
}
