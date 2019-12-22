package io.odin.slf4j

import cats.effect.IO
import cats.effect.concurrent.Ref
import io.odin.{Level, LoggerMessage, OdinSpec}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.Queue

class Slf4jSpec extends OdinSpec {

  it should "log with correct level" in {
    forAll { msgs: List[LoggerMessage] =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace(msg.message.value)
          case Level.Debug => logger.debug(msg.message.value)
          case Level.Info  => logger.info(msg.message.value)
          case Level.Warn  => logger.warn(msg.message.value)
          case Level.Error => logger.error(msg.message.value)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level)) shouldBe msgs.map(msg =>
        (msg.message.value, msg.level)
      )
    }

  }

  it should "resolve minimal level" in {
    LoggerFactory.getLogger(Level.Trace.toString).isTraceEnabled shouldBe true

    LoggerFactory.getLogger(Level.Debug.toString).isDebugEnabled shouldBe true
    LoggerFactory.getLogger(Level.Debug.toString).isTraceEnabled shouldBe false

    LoggerFactory.getLogger(Level.Info.toString).isInfoEnabled shouldBe true
    LoggerFactory.getLogger(Level.Info.toString).isDebugEnabled shouldBe false

    LoggerFactory.getLogger(Level.Warn.toString).isWarnEnabled shouldBe true
    LoggerFactory.getLogger(Level.Warn.toString).isInfoEnabled shouldBe false

    LoggerFactory.getLogger(Level.Error.toString).isErrorEnabled shouldBe true
    LoggerFactory.getLogger(Level.Error.toString).isWarnEnabled shouldBe false
  }

  it should "format logs" in {
    forAll { msgs: List[LoggerMessage] =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace("{}", msg.message.value)
          case Level.Debug => logger.debug("{}", msg.message.value)
          case Level.Info  => logger.info("{}", msg.message.value)
          case Level.Warn  => logger.warn("{}", msg.message.value)
          case Level.Error => logger.error("{}", msg.message.value)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level)) shouldBe msgs.map(msg =>
        (msg.message.value, msg.level)
      )
    }
  }

  it should "format logs with two arguments" in {
    forAll { (msgs: List[LoggerMessage], i: Int) =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace("{} {}", msg.message.value, i)
          case Level.Debug => logger.debug("{} {}", msg.message.value, i)
          case Level.Info  => logger.info("{} {}", msg.message.value, i)
          case Level.Warn  => logger.warn("{} {}", msg.message.value, i)
          case Level.Error => logger.error("{} {}", msg.message.value, i)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level)) shouldBe msgs.map(msg =>
        (s"${msg.message.value} $i", msg.level)
      )
    }
  }

  it should "format logs with multiple arguments" in {
    forAll { (msgs: List[LoggerMessage], i: Int, i2: Int) =>
      val (logger, buffer) = getLogger
      msgs.foreach { msg =>
        msg.level match {
          case Level.Trace => logger.trace("{} {} {}", msg.message.value, i, i2)
          case Level.Debug => logger.debug("{} {} {}", msg.message.value, i, i2)
          case Level.Info  => logger.info("{} {} {}", msg.message.value, i, i2)
          case Level.Warn  => logger.warn("{} {} {}", msg.message.value, i, i2)
          case Level.Error => logger.error("{} {} {}", msg.message.value, i, i2)
        }
      }

      buffer.get.unsafeRunSync().map(msg => (msg.message.value, msg.level)) shouldBe msgs.map(msg =>
        (s"${msg.message.value} $i $i2", msg.level)
      )
    }
  }

  def getLogger: (Logger, Ref[IO, Queue[LoggerMessage]]) = {
    val logger = LoggerFactory.getLogger(this.getClass).asInstanceOf[OdinLoggerAdapter[IO]]
    (logger, logger.underlying.asInstanceOf[BufferingLogger[IO]].buffer)
  }

}
