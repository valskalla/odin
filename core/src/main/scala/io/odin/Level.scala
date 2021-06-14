package io.odin

import cats.kernel.{LowerBounded, PartialOrder, UpperBounded}
import cats.{Order, Show}

/**
  * Message log level
  */
sealed trait Level

object Level {
  case object Trace extends Level {
    override val toString: String = "TRACE"
  }

  case object Debug extends Level {
    override val toString: String = "DEBUG"
  }

  case object Info extends Level {
    override val toString: String = "INFO"
  }

  case object Warn extends Level {
    override val toString: String = "WARN"
  }

  case object Error extends Level {
    override val toString: String = "ERROR"
  }

  implicit val show: Show[Level] = Show.fromToString[Level]

  implicit val order: Order[Level] with LowerBounded[Level] with UpperBounded[Level] =
    new Order[Level] with LowerBounded[Level] with UpperBounded[Level] { self =>
      private def f: Level => Int = {
        case Error => 4
        case Warn  => 3
        case Info  => 2
        case Debug => 1
        case Trace => 0
      }

      def compare(x: Level, y: Level): Int = f(x).compare(f(y))

      def minBound: Level = Level.Trace

      def maxBound: Level = Level.Error

      def partialOrder: PartialOrder[Level] = self
    }
}
