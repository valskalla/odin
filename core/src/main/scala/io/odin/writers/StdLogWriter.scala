package io.odin.writers

import java.io.PrintStream

import cats.effect.{ContextShift, Sync}
import io.odin.LoggerMessage
import io.odin.formatter.Formatter

import scala.concurrent.ExecutionContext

object StdLogWriter {

  /**
    * Logger that writes formatted logs to the given `PrintStream` by allocating threads from
    * provided `ExecutionContext`
    * @param out stream to write information into
    * @param ec thread pool that will be used for writing into stream
    */
  def mk[F[_]](
      out: PrintStream,
      ec: ExecutionContext = unboundedExecutionContext
  )(implicit F: Sync[F], contextShift: ContextShift[F]): LogWriter[F] =
    (msg: LoggerMessage, formatter: Formatter) =>
      contextShift.evalOn(ec) {
        F.delay(out.println(formatter.format(msg)))
      }

}
