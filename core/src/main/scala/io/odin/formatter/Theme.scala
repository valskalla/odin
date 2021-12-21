package io.odin.formatter

import scala.io.AnsiColor._

case class Theme(
    reset: String,
    timestamp: String,
    context: String,
    threadName: String,
    level: String,
    position: String,
    exception: String
)

object Theme {
  val BRIGHT_BLACK = "\u001b[30;1m"

  val ansi: Theme = Theme(
    reset = RESET,
    timestamp = WHITE,
    context = MAGENTA,
    threadName = GREEN,
    level = BRIGHT_BLACK,
    position = BLUE,
    exception = RED
  )
}
