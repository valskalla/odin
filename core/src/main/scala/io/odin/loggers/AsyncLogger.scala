package io.odin.loggers

import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.effect.std.Queue
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage}

import scala.concurrent.duration._

/**
  * AsyncLogger spawns non-cancellable `cats.effect.Fiber` with actual log action encapsulated there.
  *
  * Use `AsyncLogger.withAsync` to instantiate it safely
  */
case class AsyncLogger[F[_]](queue: Queue[F, LoggerMessage], timeWindow: FiniteDuration, inner: Logger[F])(
    implicit F: Async[F]
) extends DefaultLogger[F](inner.minLevel) {
  def submit(msg: LoggerMessage): F[Unit] = {
    queue.tryOffer(msg).void
  }

  def drain: F[Unit] = {
    drainAll
      .flatMap { msgs =>
        inner.log(msgs.toList)
      }
      .orElse(F.unit)
  }

  /**
    * Run internal loop of consuming events from the queue and push them down the chain
    */
  def runF: Resource[F, Unit] = {
    def drainLoop: F[Unit] = F.andWait(drain, timeWindow).foreverM[Unit]

    Resource.make(F.start(drainLoop))(fiber => drain >> fiber.cancel).void
  }

  def withMinimalLevel(level: Level): Logger[F] = copy(inner = inner.withMinimalLevel(level))

  private def drainAll: F[Vector[LoggerMessage]] =
    F.tailRecM(Vector.empty[LoggerMessage]) { acc =>
      queue.tryTake.map {
        case Some(value) => Left(acc :+ value)
        case None        => Right(acc)
      }
    }
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
        Queue.bounded[F, LoggerMessage](value)
      case None =>
        Queue.unbounded[F, LoggerMessage]
    }

    for {
      queue <- Resource.eval(createQueue)
      logger <- Resource.pure(AsyncLogger(queue, timeWindow, inner))
      _ <- logger.runF
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
