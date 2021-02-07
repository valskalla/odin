package io.odin.loggers

import cats.Monad
import cats.data.WriterT
import cats.effect.Clock
import io.odin.LoggerMessage

/**
  * Pure logger that stores logs in `WriterT` log
  */
class WriterTLogger[F[_]: Clock: Monad] extends DefaultLogger[WriterT[F, List[LoggerMessage], *]] {
  def submit(msg: LoggerMessage): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(List(msg))

  override def submit(msgs: List[LoggerMessage]): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(msgs)
}
