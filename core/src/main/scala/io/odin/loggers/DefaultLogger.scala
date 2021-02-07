package io.odin.loggers

import java.util.concurrent.TimeUnit

import cats.{Eval, Monad}
import cats.effect.Clock
import cats.syntax.all._
import io.odin.meta.{Position, Render, ToThrowable}
import io.odin.{Level, Logger, LoggerMessage}

/**
  * Default logger that relies on implicits of `Clock[F]` and `Monad[F]` to get timestamp and create log
  * message with this timestamp
  */
abstract class DefaultLogger[F[_]](val minLevel: Level = Level.Trace)(implicit clock: Clock[F], F: Monad[F])
    extends Logger[F] { self =>
  private def log[M](level: Level, msg: => M, ctx: Map[String, String] = Map.empty, t: Option[Throwable] = None)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] =
    for {
      timestamp <- clock.realTime(TimeUnit.MILLISECONDS)
      _ <- log(
        LoggerMessage(
          level = level,
          message = Eval.later(render.render(msg)),
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

  def submit(msg: LoggerMessage): F[Unit]
  def submit(msgs: List[LoggerMessage]): F[Unit] = msgs.traverse_(msg => submit(msg))

  def withMinimalLevel(level: Level): Logger[F] = new DefaultLogger[F](level) {
    def submit(msg: LoggerMessage): F[Unit] = self.submit(msg)
    override def submit(msgs: List[LoggerMessage]): F[Unit] = self.submit(msgs)
  }

  def log(msg: LoggerMessage): F[Unit] = F.whenA(msg.level >= minLevel)(self.submit(msg))

  def log(msgs: List[LoggerMessage]): F[Unit] =
    submit(msgs.filter(_.level >= minLevel))

  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Trace) {
      log(Level.Trace, msg)
    }

  def trace[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Trace) {
      log(Level.Trace, msg, t = Some(tt.throwable(e)))
    }

  def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Trace) {
      log(Level.Trace, msg, ctx)
    }

  def trace[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Trace) {
      log(Level.Trace, msg, ctx, Some(tt.throwable(e)))
    }

  def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Debug) {
      log(Level.Debug, msg)
    }

  def debug[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Debug) {
      log(Level.Debug, msg, t = Some(tt.throwable(e)))
    }

  def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Debug) {
      log(Level.Debug, msg, ctx)
    }

  def debug[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Debug) {
      log(Level.Debug, msg, ctx, Some(tt.throwable(e)))
    }

  def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Info) {
      log(Level.Info, msg)
    }

  def info[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Info) {
      log(Level.Info, msg, t = Some(tt.throwable(e)))
    }

  def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Info) {
      log(Level.Info, msg, ctx)
    }

  def info[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Info) {
      log(Level.Info, msg, ctx, Some(tt.throwable(e)))
    }

  def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Warn) {
      log(Level.Warn, msg)
    }

  def warn[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Warn) {
      log(Level.Warn, msg, t = Some(tt.throwable(e)))
    }

  def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Warn) {
      log(Level.Warn, msg, ctx)
    }

  def warn[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Warn) {
      log(Level.Warn, msg, ctx, Some(tt.throwable(e)))
    }

  def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Error) {
      log(Level.Error, msg)
    }

  def error[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Error) {
      log(Level.Error, msg, t = Some(tt.throwable(e)))
    }

  def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
    F.whenA(minLevel <= Level.Error) {
      log(Level.Error, msg, ctx)
    }

  def error[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] =
    F.whenA(minLevel <= Level.Error) {
      log(Level.Error, msg, ctx, Some(tt.throwable(e)))
    }
}
