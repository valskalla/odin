package io.odin.loggers

import cats.Id
import cats.data.Writer
import cats.effect.Clock
import cats.effect.Timer
import cats.instances.list._
import cats.syntax.all._
import io.odin.Level
import io.odin.Logger
import io.odin.LoggerMessage
import io.odin.OdinSpec

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.TimeUnit

class DefaultLoggerSpec extends OdinSpec {
  type F[A] = Writer[List[LoggerMessage], A]

  it should "correctly construct LoggerMessage" in {
    forAll { (msg: String, ctx: Map[String, String], throwable: Throwable, timestamp: Long) =>
      implicit val clk: Timer[Id] = clock(timestamp)
      val log = logger.withMinimalLevel(Level.Trace)
      check(log.trace(msg))(Level.Trace, msg, timestamp)
      check(log.trace(msg, throwable))(Level.Trace, msg, timestamp, throwable = Some(throwable))
      check(log.trace(msg, ctx))(Level.Trace, msg, timestamp, ctx)
      check(log.trace(msg, ctx, throwable))(Level.Trace, msg, timestamp, ctx, Some(throwable))

      check(log.debug(msg))(Level.Debug, msg, timestamp)
      check(log.debug(msg, throwable))(Level.Debug, msg, timestamp, throwable = Some(throwable))
      check(log.debug(msg, ctx))(Level.Debug, msg, timestamp, ctx)
      check(log.debug(msg, ctx, throwable))(Level.Debug, msg, timestamp, ctx, Some(throwable))

      check(log.info(msg))(Level.Info, msg, timestamp)
      check(log.info(msg, throwable))(Level.Info, msg, timestamp, throwable = Some(throwable))
      check(log.info(msg, ctx))(Level.Info, msg, timestamp, ctx)
      check(log.info(msg, ctx, throwable))(Level.Info, msg, timestamp, ctx, Some(throwable))

      check(log.warn(msg))(Level.Warn, msg, timestamp)
      check(log.warn(msg, throwable))(Level.Warn, msg, timestamp, throwable = Some(throwable))
      check(log.warn(msg, ctx))(Level.Warn, msg, timestamp, ctx)
      check(log.warn(msg, ctx, throwable))(Level.Warn, msg, timestamp, ctx, Some(throwable))

      check(log.error(msg))(Level.Error, msg, timestamp)
      check(log.error(msg, throwable))(Level.Error, msg, timestamp, throwable = Some(throwable))
      check(log.error(msg, ctx))(Level.Error, msg, timestamp, ctx)
      check(log.error(msg, ctx, throwable))(Level.Error, msg, timestamp, ctx, Some(throwable))
    }
  }

  it should "write multiple messages" in {
    forAll { msgs: List[LoggerMessage] =>
      implicit val clk: Timer[Id] = clock(0L)
      logger.withMinimalLevel(Level.Trace).log(msgs).written shouldBe msgs
    }
  }

  it should "filter by minimal level" in {
    implicit val clk: Timer[Id] = clock(0)
    forAll { (minLevel: Level, msgLevel: Level, msg: String) =>
      val l = logger.withMinimalLevel(minLevel)
      val fn = levelToFn(l, msgLevel) _
      val written: Id[List[LoggerMessage]] = fn(msg).written
      if (msgLevel >= minLevel) {
        written should not have size(0)
      } else {
        written should have size (0)
      }
    }
  }

  private def levelToFn(logger: Logger[F], level: Level)(msg: String): F[Unit] = level match {
    case Level.Trace => logger.trace(msg)
    case Level.Debug => logger.debug(msg)
    case Level.Info  => logger.info(msg)
    case Level.Warn  => logger.warn(msg)
    case Level.Error => logger.error(msg)
  }

  private def clock(timestamp: Long): Timer[Id] = new Timer[Id] {
    def clock: Clock[Id] = new Clock[Id] {
      def realTime(unit: TimeUnit): Id[Long] = timestamp
      def monotonic(unit: TimeUnit): Id[Long] = timestamp
    }

    def sleep(duration: FiniteDuration): Id[Unit] = ???
  }

  private def logger(implicit timer: Timer[Id]): Logger[F] = {
    new DefaultLogger[F] {
      def log(msg: LoggerMessage): Writer[List[LoggerMessage], Unit] = Writer.tell(List(msg))
    }
  }

  private def check(
      fn: => F[Unit]
  )(
      level: Level,
      msg: String,
      timestamp: Long,
      ctx: Map[String, String] = Map.empty,
      throwable: Option[Throwable] = None
  ) = {
    val List(loggerMessage) = fn.written
    loggerMessage.level shouldBe level
    loggerMessage.message.value shouldBe msg
    loggerMessage.context shouldBe ctx
    loggerMessage.exception shouldBe throwable
    loggerMessage.threadName shouldBe Thread.currentThread().getName
    loggerMessage.timestamp shouldBe timestamp
  }
}
