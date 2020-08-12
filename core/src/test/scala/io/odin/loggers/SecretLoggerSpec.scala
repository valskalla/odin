package io.odin.loggers

import cats.data.WriterT
import cats.effect.{IO, Timer}
import io.odin.{LoggerMessage, OdinSpec}
import io.odin.syntax._
import cats.instances.list._

class SecretLoggerSpec extends OdinSpec {

  type F[A] = WriterT[IO, List[LoggerMessage], A]

  implicit val timer: Timer[IO] = zeroTimer

  checkAll(
    "SecretLogger",
    LoggerTests[F](new WriterTLogger[IO].withSecretContext("foo"), _.written.unsafeRunSync()).all
  )

  it should "modify context by hashing secret keys" in {
    forAll { msg: List[LoggerMessage] =>

    }
  }

}
