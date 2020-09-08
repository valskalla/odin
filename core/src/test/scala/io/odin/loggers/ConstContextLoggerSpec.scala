package io.odin.loggers

import cats.data.WriterT
import cats.effect.{Clock, IO}
import io.odin.syntax._
import io.odin.{LoggerMessage, OdinSpec}

class ConstContextLoggerSpec extends OdinSpec {
  type F[A] = WriterT[IO, List[LoggerMessage], A]

  implicit val clock: Clock[IO] = zeroClock

  checkAll(
    "ContextualLogger",
    LoggerTests[F](
      new WriterTLogger[IO].withConstContext(Map.empty),
      _.written.unsafeRunSync()
    ).all
  )

  it should "add constant context to the record" in {
    forAll { (loggerMessage: LoggerMessage, ctx: Map[String, String]) =>
      val logger = new WriterTLogger[IO].withConstContext(ctx)
      val List(written) = logger.log(loggerMessage).written.unsafeRunSync()
      written.context shouldBe loggerMessage.context ++ ctx
    }
  }

  it should "add constant context to the records" in {
    forAll { (messages: List[LoggerMessage], ctx: Map[String, String]) =>
      val logger = new WriterTLogger[IO].withConstContext(ctx)
      val written = logger.log(messages).written.unsafeRunSync()
      written.map(_.context) shouldBe messages.map(_.context ++ ctx)
    }
  }
}
