package io.odin.loggers

import cats.Monad
import cats.data.WriterT
import cats.effect.kernel.Clock
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Pure logger that stores logs in `WriterT` log
  */
class WriterTLogger[F[_]: Clock: Monad](override val minLevel: Level = Level.Trace)
    extends DefaultLogger[WriterT[F, List[LoggerMessage], *]](minLevel) {
  def submit(msg: LoggerMessage): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(List(msg))

  override def submit(msgs: List[LoggerMessage]): WriterT[F, List[LoggerMessage], Unit] = WriterT.tell(msgs)

  def withMinimalLevel(level: Level): Logger[WriterT[F, List[LoggerMessage], *]] = new WriterTLogger[F](level)
}
