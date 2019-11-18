package io.odin

import cats.instances.int._
import cats.{Order, Show}

/**
  * Message log level
  */
sealed abstract class Level(val value: Int)

object Level {

  case object Trace extends Level(0) {
    override val toString: String = "TRACE"
  }

  case object Debug extends Level(1) {
    override val toString: String = "DEBUG"
  }

  case object Info extends Level(2) {
    override val toString: String = "INFO"
  }

  case object Warn extends Level(3) {
    override val toString: String = "WARN"
  }

  case object Error extends Level(4) {
    override val toString: String = "ERROR"
  }

  implicit val show: Show[Level] = Show.fromToString[Level]

  implicit val order: Order[Level] = Order.by[Level, Int](_.value)
}
