package io.odin.loggers

import cats.data.WriterT
import cats.effect.{Clock, IO}
import cats.syntax.all._
import io.odin._
import io.odin.syntax._

class FilterLoggerSpec extends OdinSpec {
  type F[A] = WriterT[IO, List[LoggerMessage], A]
  implicit val clock: Clock[IO] = zeroClock

  checkAll(
    "FilterLogger",
    LoggerTests[F](new WriterTLogger[IO].filter(_.exception.isDefined), _.written.unsafeRunSync()).all
  )

  it should "logger.filter(p).log(msg) <-> F.whenA(p)(log(msg))" in {
    forAll { (msgs: List[LoggerMessage], p: LoggerMessage => Boolean) =>
      {
        val logger = new WriterTLogger[IO].filter(p)
        val written = msgs.traverse(logger.log).written.unsafeRunSync()
        val batchWritten = logger.log(msgs).written.unsafeRunSync()

        written shouldBe msgs.filter(p)
        batchWritten shouldBe written
      }
    }
  }
}
