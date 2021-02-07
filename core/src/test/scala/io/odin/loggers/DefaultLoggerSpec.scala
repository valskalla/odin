package io.odin.loggers

import cats.Id
import cats.data.Writer
import cats.effect.Clock
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}

class DefaultLoggerSpec extends OdinSpec {
  type F[A] = Writer[List[LoggerMessage], A]

  it should "correctly construct LoggerMessage" in {
    forAll { (msg: String, ctx: Map[String, String], throwable: Throwable, timestamp: Long) =>
      implicit val clk: Clock[Id] = fixedClock(timestamp)
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
      implicit val clk: Clock[Id] = zeroClock
      logger.withMinimalLevel(Level.Trace).log(msgs).written shouldBe msgs
    }
  }

  it should "filter by minimal level" in {
    implicit val clk: Clock[Id] = zeroClock
    forAll { (minLevel: Level, msgLevel: Level, msg: String) =>
      val l = logger.withMinimalLevel(minLevel)
      val fn = levelToFn(l, msgLevel) _
      val written = fn(msg).written
      if (msgLevel >= minLevel) {
        written shouldBe Symbol("nonEmpty")
      } else {
        written shouldBe Symbol("empty")
      }
    }
  }

  it should "decrease min log level on update" in {
    implicit val clk: Clock[Id] = zeroClock
    forAll { (maxLevel: Level, minLevel: Level, msg: LoggerMessage) =>
      whenever(maxLevel > minLevel) {
        val l = logger.withMinimalLevel(maxLevel).withMinimalLevel(minLevel)
        val written = l.log(msg).written
        if (msg.level >= minLevel) {
          written shouldBe Symbol("nonEmpty")
        } else {
          written shouldBe Symbol("empty")
        }
      }
    }
  }

  it should "increase min log level on update" in {
    implicit val clk: Clock[Id] = zeroClock
    forAll { (maxLevel: Level, minLevel: Level, msg: LoggerMessage) =>
      whenever(maxLevel > minLevel) {
        val l = logger.withMinimalLevel(minLevel).withMinimalLevel(maxLevel)
        val written = l.log(msg).written
        if (msg.level >= maxLevel) {
          written shouldBe Symbol("nonEmpty")
        } else {
          written shouldBe Symbol("empty")
        }
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

  private def logger(implicit clock: Clock[Id]): Logger[F] = {
    new DefaultLogger[F] {
      def submit(msg: LoggerMessage): Writer[List[LoggerMessage], Unit] = Writer.tell(List(msg))
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
