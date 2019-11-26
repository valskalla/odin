package io.odin

import cats.syntax.all._
import cats.{~>, Applicative, Monoid}
import io.odin.meta.{Position, Render}

trait Logger[F[_]] {
  def log(msg: LoggerMessage): F[Unit]

  def log(msgs: List[LoggerMessage]): F[Unit]

  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def trace[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit]

  def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def debug[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit]

  def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def info[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit]

  def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def warn[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit]

  def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def error[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit]
}

object Logger extends Noop with LoggerInstances {
  implicit class LoggerOps[F[_]](logger: Logger[F]) {
    def mapK[G[_]](f: F ~> G): Logger[G] = new Logger[G] {
      def log(msg: LoggerMessage): G[Unit] = f(logger.log(msg))

      def log(msgs: List[LoggerMessage]): G[Unit] = f(logger.log(msgs))

      def trace[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] = f(logger.trace(msg))

      def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.trace(msg, t))

      def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.trace(msg, ctx))

      def trace[M](msg: => M, ctx: Map[String, String], t: Throwable)(
          implicit render: Render[M],
          position: Position
      ): G[Unit] =
        f(logger.trace(msg, ctx, t))

      def debug[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(msg))

      def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(msg, t))

      def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(msg, ctx))

      def debug[M](msg: => M, ctx: Map[String, String], t: Throwable)(
          implicit render: Render[M],
          position: Position
      ): G[Unit] =
        f(logger.debug(msg, ctx, t))

      def info[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(msg))

      def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(msg, t))

      def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(msg, ctx))

      def info[M](msg: => M, ctx: Map[String, String], t: Throwable)(
          implicit render: Render[M],
          position: Position
      ): G[Unit] =
        f(logger.info(msg, ctx, t))

      def warn[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(msg))

      def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(msg, t))

      def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(msg, ctx))

      def warn[M](msg: => M, ctx: Map[String, String], t: Throwable)(
          implicit render: Render[M],
          position: Position
      ): G[Unit] =
        f(logger.warn(msg, ctx, t))

      def error[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(msg))

      def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(msg, t))

      def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(msg, ctx))

      def error[M](msg: => M, ctx: Map[String, String], t: Throwable)(
          implicit render: Render[M],
          position: Position
      ): G[Unit] =
        f(logger.error(msg, ctx, t))
    }
  }
}

trait Noop {
  def noop[F[_]](implicit F: Applicative[F]): Logger[F] = new NoopLogger[F]
}

trait LoggerInstances {
  implicit def monoidLogger[F[_]: Applicative]: Monoid[Logger[F]] = new MonoidLogger[F]
}

private[odin] class NoopLogger[F[_]](implicit F: Applicative[F]) extends Logger[F] {
  def log(msg: LoggerMessage): F[Unit] = F.unit

  def log(msgs: List[LoggerMessage]): F[Unit] = F.unit

  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def trace[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] = F.unit

  def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] = F.unit

  def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] = F.unit

  def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] = F.unit

  def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M](msg: => M, ctx: Map[String, String], t: Throwable)(
      implicit render: Render[M],
      position: Position
  ): F[Unit] = F.unit
}

private[odin] class MonoidLogger[F[_]: Applicative] extends Monoid[Logger[F]] {
  val empty: Logger[F] = Logger.noop

  def combine(x: Logger[F], y: Logger[F]): Logger[F] = new Logger[F] {
    def log(msg: LoggerMessage): F[Unit] =
      x.log(msg) *> y.log(msg)

    def log(msgs: List[LoggerMessage]): F[Unit] = x.log(msgs) *> y.log(msgs)

    def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(msg) *> y.trace(msg)

    def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(msg, t) *> y.trace(msg, t)

    def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(msg, ctx) *> y.trace(msg, ctx)

    def trace[M](msg: => M, ctx: Map[String, String], t: Throwable)(
        implicit render: Render[M],
        position: Position
    ): F[Unit] =
      x.trace(msg, ctx, t) *> y.trace(msg, ctx, t)

    def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(msg) *> y.debug(msg)

    def debug[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(msg, t) *> y.debug(msg, t)

    def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(msg, ctx) *> y.debug(msg, ctx)

    def debug[M](msg: => M, ctx: Map[String, String], t: Throwable)(
        implicit render: Render[M],
        position: Position
    ): F[Unit] =
      x.debug(msg, ctx, t) *> y.debug(msg, ctx, t)

    def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.info(msg) *> y.info(msg)

    def info[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.info(msg, t) *> y.info(msg, t)

    def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.info(msg, ctx) *> y.info(msg, ctx)

    def info[M](msg: => M, ctx: Map[String, String], t: Throwable)(
        implicit render: Render[M],
        position: Position
    ): F[Unit] =
      x.info(msg, ctx, t) *> y.info(msg, ctx, t)

    def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(msg) *> y.warn(msg)

    def warn[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(msg, t) *> y.warn(msg, t)

    def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(msg, ctx) *> y.warn(msg, ctx)

    def warn[M](msg: => M, ctx: Map[String, String], t: Throwable)(
        implicit render: Render[M],
        position: Position
    ): F[Unit] =
      x.warn(msg, ctx, t) *> y.warn(msg, ctx, t)

    def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.error(msg) *> y.error(msg)

    def error[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit] =
      x.error(msg, t) *> y.error(msg, t)

    def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.error(msg, ctx) *> y.error(msg, ctx)

    def error[M](msg: => M, ctx: Map[String, String], t: Throwable)(
        implicit render: Render[M],
        position: Position
    ): F[Unit] =
      x.error(msg, ctx, t) *> y.error(msg, ctx, t)
  }
}
