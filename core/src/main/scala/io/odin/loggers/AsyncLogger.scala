package io.odin.loggers

import cats.MonadThrow
import cats.effect.kernel.{Async, Clock, MonadCancelThrow, Resource}
import cats.effect.std.{Dispatcher, Queue, Semaphore}
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage}

import scala.concurrent.duration._

/**
  * AsyncLogger spawns non-cancellable `cats.effect.Fiber` with actual log action encapsulated there.
  *
  * Use `AsyncLogger.withAsync` to instantiate it safely
  */
case class AsyncLogger[F[_]: Clock](
    queue: Queue[F, F[Unit]],
    sem: Semaphore[F],
    timeWindow: FiniteDuration,
    inner: Logger[F]
)(implicit F: MonadCancelThrow[F])
    extends DefaultLogger[F](inner.minLevel) {

  def submit(msg: LoggerMessage): F[Unit] =
    queue.tryOffer(inner.log(msg)).void

  private[loggers] def drain: F[Unit] =
    sem.permit.use(_ => drainAll.orElse(F.unit))

  def withMinimalLevel(level: Level): Logger[F] =
    copy(inner = inner.withMinimalLevel(level))

  private def drainAll: F[Unit] =
    queue.tryTake
      .flatMap[Option[Unit]] {
        case Some(log) =>
          log.as(None)
        case None =>
          MonadThrow[F].pure(Some(()))
      }
      .untilDefinedM

}

object AsyncLogger {

  /**
    * Create async logger and start internal loop of sending events down the chain from the buffer once
    * `Resource` is used.
    *
    * @param inner logger that will receive messages from the buffer
    * @param timeWindow pause between buffer flushing
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events might be dropped until there is a space in the buffer.
    */
  def withAsync[F[_]](
      inner: Logger[F],
      timeWindow: FiniteDuration,
      maxBufferSize: Option[Int]
  )(
      implicit F: Async[F]
  ): Resource[F, Logger[F]] = {
    val createQueue = maxBufferSize match {
      case Some(value) =>
        Queue.bounded[F, F[Unit]](value)
      case None =>
        Queue.unbounded[F, F[Unit]]
    }

    // Run internal loop of consuming events from the queue and push them down the chain
    def backgroundConsumer(logger: AsyncLogger[F]): Resource[F, Unit] = {
      def drainLoop: F[Unit] = F.andWait(logger.drain, timeWindow).foreverM[Unit]

      Resource.make(F.start(drainLoop))(fiber => logger.drain >> fiber.cancel).void
    }

    for {
      queue <- Resource.eval(createQueue)
      sem <- Resource.eval(Semaphore[F](1))
      logger <- Resource.pure(AsyncLogger(queue, sem, timeWindow, inner))
      _ <- backgroundConsumer(logger)
    } yield logger
  }

  /**
    * Create async logger and start internal loop of sending events down the chain from the buffer right away
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events will be dropped until there is a space in the buffer.
    */
  def withAsyncUnsafe[F[_]](
      inner: Logger[F],
      timeWindow: FiniteDuration,
      maxBufferSize: Option[Int]
  )(
      implicit F: Async[F],
      dispatcher: Dispatcher[F]
  ): Logger[F] = dispatcher.unsafeRunSync(withAsync(inner, timeWindow, maxBufferSize).allocated)._1
}
