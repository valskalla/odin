package io

import cats.effect.Sync
import cats.effect.Timer
import io.odin.formatter.Formatter
import io.odin.loggers.ConsoleLogger

package object odin {

  /**
    * Basic console logger that prints to STDOUT & STDERR
    * @param formatter formatter to use for log messages
    * @param minLevel minimal level of logs to be printed
    */
  def consoleLogger[F[_]: Sync: Timer](
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Logger[F] =
    ConsoleLogger(formatter, minLevel)

}
