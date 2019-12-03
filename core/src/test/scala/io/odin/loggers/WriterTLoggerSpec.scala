package io.odin.loggers

import cats.Id
import cats.data.Writer
import cats.effect.Timer
import cats.instances.list._
import io.odin.{LoggerMessage, OdinSpec}

class WriterTLoggerSpec extends OdinSpec {
  type F[A] = Writer[List[LoggerMessage], A]

  implicit val timer: Timer[Id] = zeroTimer

  checkAll(
    "WriterTLogger",
    LoggerTests[F](new WriterTLogger[Id], _.written).all
  )

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
