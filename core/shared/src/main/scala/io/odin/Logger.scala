package io.odin

import cats.kernel.UpperBounded
import cats.syntax.all._
import cats.{~>, Applicative, Monoid}
import io.odin.meta.{Position, Render, ToThrowable}

trait Logger[F[_]] {
  def minLevel: Level

  def withMinimalLevel(level: Level): Logger[F]

  def log(msg: LoggerMessage): F[Unit]

  def log(msgs: List[LoggerMessage]): F[Unit]

  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def trace[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit]

  def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def trace[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit]

  def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def debug[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit]

  def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def debug[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit]

  def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def info[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit]

  def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def info[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit]

  def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def warn[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit]

  def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def warn[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit]

  def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def error[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit]

  def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  def error[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit]

}

object Logger extends Noop with LoggerInstances {

  def apply[F[_]](implicit instance: Logger[F]): Logger[F] = instance

  implicit class LoggerOps[F[_]](logger: Logger[F]) {
    def mapK[G[_]](f: F ~> G): Logger[G] = new Logger[G] {
      val minLevel: Level = logger.minLevel

      def withMinimalLevel(level: Level): Logger[G] = logger.withMinimalLevel(level).mapK(f)

      def log(msg: LoggerMessage): G[Unit] = f(logger.log(msg))

      def log(msgs: List[LoggerMessage]): G[Unit] = f(logger.log(msgs))

      def trace[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] = f(logger.trace(msg))

      def trace[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): G[Unit] =
        f(logger.trace(msg, e))

      def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.trace(msg, ctx))

      def trace[M, E](msg: => M, ctx: Map[String, String], e: E)(
          implicit render: Render[M],
          tt: ToThrowable[E],
          position: Position
      ): G[Unit] =
        f(logger.trace(msg, ctx, e))

      def debug[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(msg))

      def debug[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): G[Unit] =
        f(logger.debug(msg, e))

      def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.debug(msg, ctx))

      def debug[M, E](msg: => M, ctx: Map[String, String], e: E)(
          implicit render: Render[M],
          tt: ToThrowable[E],
          position: Position
      ): G[Unit] =
        f(logger.debug(msg, ctx, e))

      def info[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(msg))

      def info[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): G[Unit] =
        f(logger.info(msg, e))

      def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.info(msg, ctx))

      def info[M, E](msg: => M, ctx: Map[String, String], e: E)(
          implicit render: Render[M],
          tt: ToThrowable[E],
          position: Position
      ): G[Unit] =
        f(logger.info(msg, ctx, e))

      def warn[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(msg))

      def warn[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): G[Unit] =
        f(logger.warn(msg, e))

      def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.warn(msg, ctx))

      def warn[M, E](msg: => M, ctx: Map[String, String], e: E)(
          implicit render: Render[M],
          tt: ToThrowable[E],
          position: Position
      ): G[Unit] =
        f(logger.warn(msg, ctx, e))

      def error[M](msg: => M)(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(msg))

      def error[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): G[Unit] =
        f(logger.error(msg, e))

      def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): G[Unit] =
        f(logger.error(msg, ctx))

      def error[M, E](msg: => M, ctx: Map[String, String], e: E)(
          implicit render: Render[M],
          tt: ToThrowable[E],
          position: Position
      ): G[Unit] =
        f(logger.error(msg, ctx, e))
    }
  }
}

trait Noop {
  def noop[F[_]](implicit F: Applicative[F]): Logger[F] = new NoopLogger[F]
}

trait LoggerInstances {
  implicit def monoidLogger[F[_]: Applicative]: Monoid[Logger[F]] = new MonoidLogger[F]
}

private[odin] class NoopLogger[F[_]](implicit F: Applicative[F]) extends Logger[F] { self =>
  val minLevel: Level = UpperBounded[Level].maxBound

  def withMinimalLevel(level: Level): Logger[F] = self

  def log(msg: LoggerMessage): F[Unit] = F.unit

  def log(msgs: List[LoggerMessage]): F[Unit] = F.unit

  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def trace[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] = F.unit

  def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def trace[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] = F.unit

  def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] = F.unit

  def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def debug[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] = F.unit

  def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] = F.unit

  def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def info[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] = F.unit

  def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] = F.unit

  def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def warn[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] = F.unit

  def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] = F.unit

  def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] = F.unit

  def error[M, E](msg: => M, ctx: Map[String, String], e: E)(
      implicit render: Render[M],
      tt: ToThrowable[E],
      position: Position
  ): F[Unit] = F.unit
}

private[odin] class MonoidLogger[F[_]: Applicative] extends Monoid[Logger[F]] {
  val empty: Logger[F] = Logger.noop

  def combine(x: Logger[F], y: Logger[F]): Logger[F] = new Logger[F] {
    val minLevel: Level = x.minLevel.min(y.minLevel)

    def withMinimalLevel(level: Level): Logger[F] = x.withMinimalLevel(level) |+| y.withMinimalLevel(level)

    def log(msg: LoggerMessage): F[Unit] =
      x.log(msg) *> y.log(msg)

    def log(msgs: List[LoggerMessage]): F[Unit] = x.log(msgs) *> y.log(msgs)

    def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(msg) *> y.trace(msg)

    def trace[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
      x.trace(msg, e) *> y.trace(msg, e)

    def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.trace(msg, ctx) *> y.trace(msg, ctx)

    def trace[M, E](msg: => M, ctx: Map[String, String], e: E)(
        implicit render: Render[M],
        tt: ToThrowable[E],
        position: Position
    ): F[Unit] =
      x.trace(msg, ctx, e) *> y.trace(msg, ctx, e)

    def debug[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(msg) *> y.debug(msg)

    def debug[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
      x.debug(msg, e) *> y.debug(msg, e)

    def debug[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.debug(msg, ctx) *> y.debug(msg, ctx)

    def debug[M, E](msg: => M, ctx: Map[String, String], e: E)(
        implicit render: Render[M],
        tt: ToThrowable[E],
        position: Position
    ): F[Unit] =
      x.debug(msg, ctx, e) *> y.debug(msg, ctx, e)

    def info[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.info(msg) *> y.info(msg)

    def info[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
      x.info(msg, e) *> y.info(msg, e)

    def info[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.info(msg, ctx) *> y.info(msg, ctx)

    def info[M, E](msg: => M, ctx: Map[String, String], e: E)(
        implicit render: Render[M],
        tt: ToThrowable[E],
        position: Position
    ): F[Unit] =
      x.info(msg, ctx, e) *> y.info(msg, ctx, e)

    def warn[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(msg) *> y.warn(msg)

    def warn[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
      x.warn(msg, e) *> y.warn(msg, e)

    def warn[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.warn(msg, ctx) *> y.warn(msg, ctx)

    def warn[M, E](msg: => M, ctx: Map[String, String], e: E)(
        implicit render: Render[M],
        tt: ToThrowable[E],
        position: Position
    ): F[Unit] =
      x.warn(msg, ctx, e) *> y.warn(msg, ctx, e)

    def error[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit] =
      x.error(msg) *> y.error(msg)

    def error[M, E](msg: => M, e: E)(implicit render: Render[M], tt: ToThrowable[E], position: Position): F[Unit] =
      x.error(msg, e) *> y.error(msg, e)

    def error[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit] =
      x.error(msg, ctx) *> y.error(msg, ctx)

    def error[M, E](msg: => M, ctx: Map[String, String], e: E)(
        implicit render: Render[M],
        tt: ToThrowable[E],
        position: Position
    ): F[Unit] =
      x.error(msg, ctx, e) *> y.error(msg, ctx, e)
  }
}
