package io.odin.slf4j

import cats.effect.concurrent.Ref
import cats.effect.{Clock, Sync}
import io.odin.loggers.DefaultLogger
import io.odin.{Level, LoggerMessage}

import scala.collection.immutable.Queue

class BufferingLogger[F[_]: Clock](override val minLevel: Level)(implicit F: Sync[F])
    extends DefaultLogger[F](minLevel) {

  val buffer: Ref[F, Queue[LoggerMessage]] = Ref.unsafe[F, Queue[LoggerMessage]](Queue.empty)

  def submit(msg: LoggerMessage): F[Unit] = buffer.update(_.enqueue(msg))
}
