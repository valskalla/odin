package io.odin.loggers

import cats.data.{ReaderT, WriterT}
import cats.effect.{IO, Timer}
import cats.instances.list._
import cats.mtl.instances.all._
import io.odin.{LoggerMessage, OdinSpec}

class ContextualLoggerSpec extends OdinSpec {

  type W[A] = WriterT[IO, List[LoggerMessage], A]
  type F[A] = ReaderT[W, Map[String, String], A]

  implicit val hasContext: HasContext[Map[String, String]] = (env: Map[String, String]) => env
  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)

  private val logger = new DefaultLogger[F] {
    def log(msg: LoggerMessage): F[Unit] = ReaderT.liftF(WriterT.tell(List(msg)))
  }

  it should "pick up context from F[_]" in {
    forAll { (loggerMessage: LoggerMessage, ctx: Map[String, String]) =>
      val log = ContextualLogger.withContextualLogger[F].apply(logger)
      val List(written) = log.log(loggerMessage).apply(ctx).written.unsafeRunSync()
      written.context shouldBe loggerMessage.context ++ ctx
    }
  }

}
