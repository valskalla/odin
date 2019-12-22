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

  def getLogger: (Logger, Ref[IO, Queue[LoggerMessage]]) = {
    val logger = LoggerFactory.getLogger(this.getClass).asInstanceOf[OdinLoggerAdapter[IO]]
    (logger, logger.underlying.asInstanceOf[BufferingLogger[IO]].buffer)
  }

}
