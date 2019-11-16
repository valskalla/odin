package io.odin.loggers

import cats.data.WriterT
import cats.effect.{Clock, IO, Timer}
import cats.instances.list._
import io.odin.{LoggerMessage, OdinSpec}

class ConstContextLoggerSpec extends OdinSpec {

  type F[A] = WriterT[IO, List[LoggerMessage], A]

  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
  implicit val clock: Clock[IO] = timer.clock

  def logger: DefaultLogger[F] = new DefaultLogger[F] {
    def log(msg: LoggerMessage): F[Unit] = WriterT.tell(List(msg))
  }

  it should "add constant context to the record" in {
    forAll { (loggerMessage: LoggerMessage, ctx: Map[String, String]) =>
      val log = ConstContextLogger.withConstContext[F](ctx)(logger)
      val List(written) = log.log(loggerMessage).written.unsafeRunSync()
      written.context shouldBe loggerMessage.context ++ ctx
    }
  }
}
