package io.odin.loggers

import cats.Id
import cats.effect.{Clock, Timer}
import io.odin.{LoggerMessage, OdinSpec}

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

class WriterTLoggerSpec extends OdinSpec {
  implicit val clock: Timer[Id] = new Timer[Id] {
    def clock: Clock[Id] = new Clock[Id] {
      def realTime(unit: TimeUnit): Id[Long] = 0L
      def monotonic(unit: TimeUnit): Id[Long] = 0L
    }

    def sleep(duration: FiniteDuration): Id[Unit] = ???
  }

  it should "write log into list" in {
    val logger = new WriterTLogger[Id]()
    forAll { msg: LoggerMessage =>
      logger.log(msg).written shouldBe List(msg)
    }
  }

  it should "write all the logs into list" in {
    val logger = new WriterTLogger[Id]()
    forAll { msgs: List[LoggerMessage] =>
      logger.log(msgs).written shouldBe msgs
    }
  }
}
