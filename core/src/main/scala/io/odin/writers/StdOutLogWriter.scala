package io.odin.writers

import cats.effect.Sync

object StdOutLogWriter {

  //@TODO revisit for Scala.js support later
  def apply[F[_]: Sync]: LogWriter[F] =
    StdLogWriter.mk(scala.Console.out)

}
