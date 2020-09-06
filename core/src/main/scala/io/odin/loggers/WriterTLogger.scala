package io.odin.loggers

import cats.Monad
import cats.data.WriterT
import cats.effect.Timer
import io.odin.LoggerMessage

/**
  * Pure logger that stores logs in `WriterT` log
  */
class WriterTLogger[F[_]: Timer: Monad] extends DefaultLogger[WriterT[F, List[LoggerMessage], *]] {
  def log(msg: LoggerMessage): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(List(msg))

  override def log(msgs: List[LoggerMessage]): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(msgs)
}
