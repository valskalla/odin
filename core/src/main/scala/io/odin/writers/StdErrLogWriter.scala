package io.odin.writers

import cats.effect.Sync

object StdErrLogWriter {

  //@TODO revisit for Scala.js support later
  def apply[F[_]: Sync]: LogWriter[F] = StdLogWriter.mk(scala.Console.err)

}
