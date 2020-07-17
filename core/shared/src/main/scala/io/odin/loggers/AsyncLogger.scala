package io.odin.loggers

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Fiber, Resource, Timer}
import cats.syntax.all._
import io.odin.{Logger, LoggerMessage}
import monix.catnap.ConcurrentQueue
import monix.execution.{BufferCapacity, ChannelType}

import scala.concurrent.duration._

/**
  * AsyncLogger spawns non-cancellable `cats.effect.Fiber` with actual log action encapsulated there.
  *
  * Use `AsyncLogger.withAsync` to instantiate it safely
  */
case class AsyncLogger[F[_]](queue: ConcurrentQueue[F, LoggerMessage], timeWindow: FiniteDuration, inner: Logger[F])(
    implicit F: Concurrent[F],
    timer: Timer[F],
    contextShift: ContextShift[F]
) extends DefaultLogger[F](inner.minLevel) {
  def log(msg: LoggerMessage): F[Unit] = {
    queue.tryOffer(msg).void
  }

  def drain: F[Unit] = {
    queue
      .drain(0, Int.MaxValue)
      .flatMap { msgs =>
        inner.log(msgs.toList)
      }
      .void
      .handleErrorWith { _ =>
        F.unit
      }
  }

  /**
    * Run internal loop of consuming events from the queue and push them down the chain
    */
  def runF: F[Fiber[F, Unit]] = {
    def recDrain: F[Unit] =
      drain >> timer.sleep(timeWindow) >> contextShift.shift >> recDrain
    F.start(recDrain).map { fiber =>
      Fiber(fiber.join, drain >> fiber.cancel)
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
  def withAsync[F[_]: Timer: ContextShift](
      inner: Logger[F],
      timeWindow: FiniteDuration,
      maxBufferSize: Option[Int]
  )(
      implicit F: Concurrent[F]
  ): Resource[F, Logger[F]] = {
    val queueCapacity = maxBufferSize match {
      case Some(value) =>
        BufferCapacity.Bounded(value)
      case None =>
        BufferCapacity.Unbounded()
    }
    Resource
      .make {
        for {
          queue <- ConcurrentQueue.withConfig[F, LoggerMessage](queueCapacity, ChannelType.MPSC)
          logger = AsyncLogger(queue, timeWindow, inner)
          fiber <- logger.runF
        } yield {
          (fiber, logger)
        }
      } {
        case (fiber, _) => fiber.cancel
      }
      .map {
        case (_, logger) => logger
      }
  }

  /**
    * Create async logger and start internal loop of sending events down the chain from the buffer right away
    * @param maxBufferSize If `maxBufferSize` is set to some value and buffer size grows to that value,
    *                      any new events will be dropped until there is a space in the buffer.
    */
  def withAsyncUnsafe[F[_]: Timer: ContextShift](
      inner: Logger[F],
      timeWindow: FiniteDuration,
      maxBufferSize: Option[Int]
  )(
      implicit F: ConcurrentEffect[F]
  ): Logger[F] = F.toIO(withAsync(inner, timeWindow, maxBufferSize).allocated).unsafeRunSync()._1
}
