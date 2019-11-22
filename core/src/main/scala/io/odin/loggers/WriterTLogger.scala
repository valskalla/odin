package io.odin.loggers

import cats.Monad
import cats.data.WriterT
import cats.effect.Clock
import cats.instances.list._
import io.odin.LoggerMessage

/**
  * Pure logger that stores logs in `WriterT` log
  */
class WriterTLogger[F[_]: Clock: Monad] extends DefaultLogger[WriterT[F, List[LoggerMessage], *]] {
  def log(msg: LoggerMessage): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(List(msg))
}
