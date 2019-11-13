package io.odin

import cats.{Order, Show}
import cats.instances.int._

/**
  * Message log level
  */
sealed abstract class Level(val value: Int)

object Level {

  case object Trace extends Level(0)

  case object Debug extends Level(1)

  case object Info extends Level(2)

  case object Warn extends Level(3)

  case object Error extends Level(4)

  implicit val show: Show[Level] = Show.fromToString[Level]

  implicit val order: Order[Level] = Order.by[Level, Int](_.value)
}
