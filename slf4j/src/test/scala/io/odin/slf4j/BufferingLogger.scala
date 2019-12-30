package io.odin.slf4j

import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import io.odin.{Level, LoggerMessage}
import io.odin.loggers.DefaultLogger

import scala.collection.immutable.Queue

class BufferingLogger[F[_]: Timer](override val minLevel: Level)(implicit F: Sync[F])
    extends DefaultLogger[F](minLevel) {

  val buffer: Ref[F, Queue[LoggerMessage]] = Ref.unsafe[F, Queue[LoggerMessage]](Queue.empty)

  def log(msg: LoggerMessage): F[Unit] = buffer.update(_.enqueue(msg))
}
