package io.odin.writers

import cats.effect.{ContextShift, Sync}

import scala.concurrent.ExecutionContext

object StdErrLogWriter {

  //@TODO revisit for Scala.js support later
  def apply[F[_]: Sync: ContextShift](ec: ExecutionContext = unboundedExecutionContext): LogWriter[F] =
    StdLogWriter.mk(scala.Console.err, ec)

}
