package io.odin.writers

import java.io.BufferedWriter

import scala.concurrent.duration._
import java.nio.file.{Files, Paths}

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Fiber, Timer}
import cats.syntax.all._
import io.odin.LoggerMessage
import io.odin.formatter.Formatter

import scala.concurrent.duration.FiniteDuration

/**
  * Async file logger that flushes the buffer after each `timeWindow`
  *
  * Consider to use `AsyncFileLogWriter.apply` to start flushing loop properly
  */
class AsyncFileLogWriter[F[_]](writer: BufferedWriter, timeWindow: FiniteDuration)(
    implicit F: Concurrent[F],
    timer: Timer[F],
    contextShift: ContextShift[F]
) extends LogWriter[F] {

  Runtime.getRuntime.addShutdownHook {
    new Thread {
      override def run(): Unit =
        writer.close()
    }
  }

  def write(msg: LoggerMessage, formatter: Formatter): F[Unit] = {
    F.delay {
      writer.write(formatter.format(msg) + System.lineSeparator())
    }
  }

  def flush: F[Unit] = F.delay(writer.flush())

  /**
    * Run cancellable flush loop that will close the `writer` on `F[_]` cancel.
    */
  def runFlush: F[Fiber[F, Unit]] = {
    def recFlush: F[Unit] = timer.sleep(timeWindow) >> flush >> contextShift.shift >> recFlush

    def close: F[Unit] = F.delay(writer.close())

    F.onCancel(F.start(recFlush).map { fiber =>
      Fiber(fiber.join, close >> fiber.cancel)
    }) {
      close
    }
  }

}

object AsyncFileLogWriter {

  /**
    * Safely start async file writer. Cancellation of `F[_]` will be propagated down here to safely close the buffer.
    * BEWARE that cancellation invalidates the `BufferedWriter` as well, no `write` could be performed after that.
    * @param fileName name of log file to append to
    * @param timeWindow pause between flushing log events into file
    * @return [[LogWriter]] in the context of `F[_]` that will run internal async flush loop once it's started.
    */
  def apply[F[_]: ContextShift: Timer](
      fileName: String,
      timeWindow: FiniteDuration = 1.second
  )(implicit F: Concurrent[F]): F[LogWriter[F]] =
    F.delay(new AsyncFileLogWriter[F](Files.newBufferedWriter(Paths.get(fileName)), timeWindow)).flatMap { writer =>
      writer.runFlush.map(_ => writer)
    }

  /**
    * Unsafe version of [[apply]] that runs internal flush loop in the background as a side-effect
    */
  def unsafe[F[_]: ContextShift: Timer](
      fileName: String,
      timeWindow: FiniteDuration = 1.second
  )(implicit F: ConcurrentEffect[F]): LogWriter[F] = {
    val writer = new AsyncFileLogWriter[F](Files.newBufferedWriter(Paths.get(fileName)), timeWindow)
    F.toIO(writer.runFlush.map(_ => writer)).unsafeRunAsyncAndForget()
    writer
  }

}
