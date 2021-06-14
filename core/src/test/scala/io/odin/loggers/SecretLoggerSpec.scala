package io.odin.loggers

import cats.Id
import cats.catsInstancesForId
import cats.data.Writer
import cats.effect.Clock
import io.odin.syntax._
import io.odin.{LoggerMessage, OdinSpec}

class SecretLoggerSpec extends OdinSpec {

  type F[A] = Writer[List[LoggerMessage], A]

  implicit val clock: Clock[Id] = zeroClock
  implicit val clockT: Clock[F] = zeroClock

  checkAll(
    "SecretLogger",
    LoggerTests[F](new WriterTLogger[Id].withSecretContext("foo"), _.written).all
  )

  it should "modify context by hashing secret keys of a message" in {
    forAll { (msg: LoggerMessage) =>
      whenever(msg.context.nonEmpty) {
        val keys = msg.context.keys.toList
        val logger = new WriterTLogger[Id].withSecretContext(keys.head, keys.tail: _*)
        val written :: Nil = logger.log(msg).written
        checkHashedResult(msg, written)
      }
    }
  }

  it should "modify context by hashing secret keys of messages" in {
    forAll { (msgs: List[LoggerMessage]) =>
      val keys = msgs.flatMap(_.context.keys)
      whenever(keys.nonEmpty) {
        val msgsWithContext = msgs.filter(_.context.nonEmpty)
        val logger = new WriterTLogger[Id].withSecretContext(keys.head, keys.tail: _*)
        val written = logger.log(msgsWithContext).written
        msgsWithContext.zip(written).map {
          case (origin, result) =>
            checkHashedResult(origin, result)
        }
      }
    }
  }

  def checkHashedResult(origin: LoggerMessage, written: LoggerMessage): Unit = {
    origin.context.keys should contain theSameElementsAs written.context.keys
    origin.context.values shouldNot contain theSameElementsAs written.context.values
    all(written.context.values) should startWith("secret:")
  }

}
