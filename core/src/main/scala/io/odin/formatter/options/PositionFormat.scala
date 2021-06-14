package io.odin.formatter.options

sealed trait PositionFormat

object PositionFormat {
  case object Full extends PositionFormat
  case object AbbreviatePackage extends PositionFormat
}
