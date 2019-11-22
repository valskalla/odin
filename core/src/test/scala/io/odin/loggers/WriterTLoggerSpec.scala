package io.odin.loggers

import cats.Id
import cats.effect.Clock
import cats.instances.list._
import cats.syntax.all._
import io.odin.{LoggerMessage, OdinSpec}

import scala.concurrent.duration.TimeUnit

class WriterTLoggerSpec extends OdinSpec {

  implicit val clock: Clock[Id] = new Clock[Id] {
    def realTime(unit: TimeUnit): Id[Long] = 0L
    def monotonic(unit: TimeUnit): Id[Long] = 0L
  }

  it should "write all the logs into list" in {
    val logger = new WriterTLogger[Id]()
    forAll { msgs: List[LoggerMessage] =>
      msgs.traverse(logger.log).written shouldBe msgs
    }

  }

}
