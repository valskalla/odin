package io.odin.writers

import java.io.PrintStream

import cats.effect.Sync
import io.odin.LoggerMessage
import io.odin.formatter.Formatter

object StdLogWriter {

  /**
    * Logger that writes formatted logs to the given `PrintStream` by allocating threads from
    * provided `ExecutionContext`
    * @param out stream to write information into
    */
  def mk[F[_]](
      out: PrintStream
  )(implicit F: Sync[F]): LogWriter[F] =
    (msg: LoggerMessage, formatter: Formatter) => F.delay(out.println(formatter.format(msg)))

}
