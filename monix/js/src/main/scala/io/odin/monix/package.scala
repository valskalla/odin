package io.odin

import _root_.monix.eval.Task
import io.odin.formatter.Formatter

package object monix {

  /**
    * See `io.odin.consoleLogger`
    */
  def consoleLogger(
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Logger[Task] =
    io.odin.consoleLogger[Task](formatter, minLevel)

}
