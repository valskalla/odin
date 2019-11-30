package io.odin.loggers

import java.util.concurrent.TimeUnit

import cats.Monad
import cats.effect.Timer
import cats.instances.all._
import cats.syntax.all._
import io.odin.meta.{Position, Render}
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Default logger that relies on implicits of `Timer[F]` and `Monad[F]` to get timestamp and create log
  * message with this timestamp
  */
abstract class DefaultLogger[F[_]](val minLevel: Level = Level.Debug)(implicit timer: Timer[F], F: Monad[F])
    extends Logger[F] { self =>
  private def log[M](level: Level, msg: => M, ctx: Map[String, String] = Map.empty, t: Option[Throwable] = None)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] =
    F.whenA(level >= minLevel) {
      for {
        timestamp <- timer.clock.realTime(TimeUnit.MILLISECONDS)
        _ <- log(
          LoggerMessage(
            level = level,
            message = () => render.render(msg),
            context = ctx,
            exception = t,
            position = position,
            threadName = Thread.currentThread().getName,
            timestamp = timestamp
          )
        )
      } yield {
        ()
      }
    }

  def withMinimalLevel(level: Level): Logger[F] = new DefaultLogger[F](level) {
    def log(msg: LoggerMessage): F[Unit] = self.log(msg)
    override def log(msgs: List[LoggerMessage]): F[Unit] = self.log(msgs)
  }

  def log(msgs: List[LoggerMessage]): F[Unit] =
    msgs.traverse(msg => log(msg)).void

  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Trace) {
      log(Level.Trace, msg)
    }

  def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Trace) {
      log(Level.Trace, msg, t = Some(t))
    }

  def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Trace) {
      log(Level.Trace, msg, ctx)
    }

  def trace[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Trace) {
      log(Level.Trace, msg, ctx, Some(t))
    }

  def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Debug) {
      log(Level.Debug, msg)
    }

  def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Debug) {
      log(Level.Debug, msg, t = Some(t))
    }

  def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Debug) {
      log(Level.Debug, msg, ctx)
    }

  def debug[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Debug) {
      log(Level.Debug, msg, ctx, Some(t))
    }

  def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Info) {
      log(Level.Info, msg)
    }

  def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Info) {
      log(Level.Info, msg, t = Some(t))
    }

  def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Info) {
      log(Level.Info, msg, ctx)
    }

  def info[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Info) {
      log(Level.Info, msg, ctx, Some(t))
    }

  def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Warn) {
      log(Level.Warn, msg)
    }

  def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Warn) {
      log(Level.Warn, msg, t = Some(t))
    }

  def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Warn) {
      log(Level.Warn, msg, ctx)
    }

  def warn[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Warn) {
      log(Level.Warn, msg, ctx, Some(t))
    }

  def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Error) {
      log(Level.Error, msg)
    }

  def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Error) {
      log(Level.Error, msg, t = Some(t))
    }

  def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Error) {
      log(Level.Error, msg, ctx)
    }

  def error[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Error) {
      log(Level.Error, msg, ctx, t = Some(t))
    }
}
