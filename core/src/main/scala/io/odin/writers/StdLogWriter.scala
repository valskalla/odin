package io.odin.writers

import java.io.PrintStream

import cats.effect.Sync
import io.odin.LoggerMessage
import io.odin.formatter.Formatter

object StdLogWriter {

  def mk[F[_]](out: PrintStream)(implicit F: Sync[F]): LogWriter[F] =
    (msg: LoggerMessage, formatter: Formatter) => F.delay(out.println(formatter.format(msg)))

}
