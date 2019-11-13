package io.odin

import cats.syntax.all._
import cats.{Applicative, Monoid, MonoidK, ~>}
import io.odin.meta.{Position, Render}

trait Logger[F[_]] {

  def log(msg: LoggerMessage): F[Unit]

  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def trace[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def trace[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def debug[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def debug[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def info[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def info[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def warn[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def warn[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def error[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def error[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit]

}

object Logger extends Noop with LoggerInstances {

  implicit class LoggerOps[F[_]](logger: Logger[F]) {
    def mapK[G[_]](f: F ~> G): Logger[G] = new Logger[G] {
      def log(msg: LoggerMessage): G[Unit] = f(logger.log(msg))

      def trace[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] = f(logger.trace(msg))

      def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.trace(msg, t))

      def trace[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.trace(ctx)(msg))

      def trace[M](ctx: Map[String, String], t: Throwable)(
          msg: => M
      )(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.trace(ctx, t)(msg))

      def debug[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(msg))

      def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(msg, t))

      def debug[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(ctx)(msg))

      def debug[M](ctx: Map[String, String], t: Throwable)(
          msg: => M
      )(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(ctx, t)(msg))

      def info[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(msg))

      def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(msg, t))

      def info[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(ctx)(msg))

      def info[M](ctx: Map[String, String], t: Throwable)(
          msg: => M
      )(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(ctx, t)(msg))

      def warn[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(msg))

      def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(msg, t))

      def warn[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(ctx)(msg))

      def warn[M](ctx: Map[String, String], t: Throwable)(
          msg: => M
      )(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(ctx, t)(msg))

      def error[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(msg))

      def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(msg, t))

      def error[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(ctx)(msg))

      def error[M](ctx: Map[String, String], t: Throwable)(
          msg: => M
      )(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(ctx, t)(msg))
    }
  }

}

trait Noop {
  def noop[F[_]](implicit F: Applicative[F]): Logger[F] = new NoopLogger[F]
}

trait LoggerInstances {
  def monoid[F[_]: Applicative]: Monoid[Logger[F]] = new MonoidLogger[F]
}

private[odin] class NoopLogger[F[_]](implicit F: Applicative[F]) extends Logger[F] {
  def log(msg: LoggerMessage): F[Unit] = F.unit

  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def trace[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def trace[M](ctx: Map[String, String], t: Throwable)(
      msg: => M
  )(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M](
      ctx: Map[String, String],
      t: Throwable
  )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit
}

private[odin] class MonoidLogger[F[_]: Applicative] extends Monoid[Logger[F]] {
  val empty: Logger[F] = Logger.noop

  def combine(x: Logger[F], y: Logger[F]): Logger[F] = new Logger[F] {
    def log(msg: LoggerMessage): F[Unit] =
      x.log(msg) *> y.log(msg)

    def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(msg) *> y.trace(msg)

    def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(msg, t) *> y.trace(msg, t)

    def trace[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(ctx)(msg) *> y.trace(ctx)(msg)

    def trace[M](
        ctx: Map[String, String],
        t: Throwable
    )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(ctx, t)(msg) *> y.trace(ctx, t)(msg)

    def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(msg) *> y.debug(msg)

    def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(msg, t) *> y.debug(msg, t)

    def debug[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(ctx)(msg) *> y.debug(ctx)(msg)

    def debug[M](
        ctx: Map[String, String],
        t: Throwable
    )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(ctx, t)(msg) *> y.debug(ctx, t)(msg)

    def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.info(msg) *> y.info(msg)

    def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.info(msg, t) *> y.info(msg, t)

    def info[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.info(ctx)(msg) *> y.info(ctx)(msg)

    def info[M](
        ctx: Map[String, String],
        t: Throwable
    )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.info(ctx, t)(msg) *> y.info(ctx, t)(msg)

    def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(msg) *> y.warn(msg)

    def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(msg, t) *> y.warn(msg, t)

    def warn[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(ctx)(msg) *> y.warn(ctx)(msg)

    def warn[M](
        ctx: Map[String, String],
        t: Throwable
    )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(ctx, t)(msg) *> y.warn(ctx, t)(msg)

    def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.error(msg) *> y.error(msg)

    def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.error(msg, t) *> y.error(msg, t)

    def error[M](ctx: Map[String, String])(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.error(ctx)(msg) *> y.error(ctx)(msg)

    def error[M](
        ctx: Map[String, String],
        t: Throwable
    )(msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.error(ctx, t)(msg) *> y.error(ctx, t)(msg)
  }
}
