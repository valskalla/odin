package io.odin.extras.loggers

import cats.MonadError
import cats.effect.{Clock, Concurrent, ContextShift, ExitCase, Resource}
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.order._
import io.odin.loggers.DefaultLogger
import io.odin.{Level, Logger, LoggerMessage}
import monix.catnap.ConcurrentQueue
import monix.execution.{BufferCapacity, ChannelType}

final case class ConditionalLogger[F[_]: Clock] private (
    queue: ConcurrentQueue[F, LoggerMessage],
    inner: Logger[F],
    override val minLevel: Level
)(implicit F: MonadError[F, Throwable])
    extends DefaultLogger[F](minLevel) {

  def log(msg: LoggerMessage): F[Unit] =
    queue.tryOffer(msg).void

  private def drain(exitCase: ExitCase[Throwable]): F[Unit] = {
    val level = exitCase match {
      case ExitCase.Completed => inner.minLevel
      case _                  => minLevel
    }

    queue
      .drain(0, Int.MaxValue)
      .flatMap(msgs => inner.log(msgs.filter(_.level >= level).toList))
      .attempt
      .void
  }

}

object ConditionalLogger {

  /**
    * Create ConditionalLogger that buffers messages and sends them to the inner logger when the resource is released.
    * If evaluation of the bracket completed with an error, the `fallbackLevel` is used as a `minLevel`.
    *
    * Example:
    * {{{
    *   consoleLogger[F](minLevel = Level.Info).withErrorLevel(Level.Debug) { logger =>
    *     logger.debug("debug message") >> trickyCode
    *   }
    * }}}
    *
    * If evaluation completed with an error, the messages with `level >= Level.Debug` will be sent to an inner logger.
    * If evaluation completed successfully, the messages with `level >= Level.Info` will be sent to an inner logger.
    *
    *
    * '''Important:''' nothing is logged until the resource is released.
    * Example:
    * {{{
    * consoleLogger[F](minLevel = Level.Info).withErrorLevel(Level.Debug) { logger =>
    *   logger.info("info log") >> Timer[F].sleep(10.seconds) >> logger.debug("debug log")
    * }
    * }}}
    *
    * The message will be logged after 10 seconds. Thus use the logger with caution.
    *
    * @param inner logger that will receive messages from the buffer
    * @param minLevelOnError min level that will be used in case of error
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events might be dropped until there is a space in the buffer.
    */
  def create[F[_]: Clock: Concurrent: ContextShift](
      inner: Logger[F],
      minLevelOnError: Level,
      maxBufferSize: Option[Int]
  ): Resource[F, Logger[F]] = {

    val queueCapacity = maxBufferSize match {
      case Some(value) => BufferCapacity.Bounded(value)
      case None        => BufferCapacity.Unbounded()
    }

    def acquire: F[ConditionalLogger[F]] =
      for {
        queue <- ConcurrentQueue.withConfig[F, LoggerMessage](queueCapacity, ChannelType.MPSC)
      } yield ConditionalLogger(queue, inner, minLevelOnError)

    def release(logger: ConditionalLogger[F], exitCase: ExitCase[Throwable]): F[Unit] =
      logger.drain(exitCase)

    Resource.makeCase(acquire)(release).widen
  }

}
