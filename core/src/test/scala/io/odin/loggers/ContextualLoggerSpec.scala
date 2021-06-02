package io.odin.loggers

import java.util.concurrent.Executors

import cats.arrow.FunctionK
import cats.data.{ReaderT, WriterT}
import cats.effect.unsafe.IORuntime
import cats.effect.{Clock, IO}
import io.odin.syntax._
import io.odin.{LoggerMessage, OdinSpec}

import scala.concurrent.ExecutionContext

class ContextualLoggerSpec extends OdinSpec {
  type W[A] = WriterT[IO, List[LoggerMessage], A]
  type F[A] = ReaderT[W, Map[String, String], A]

  implicit val hasContext: HasContext[Map[String, String]] = (env: Map[String, String]) => env
  implicit val clock: Clock[IO] = zeroClock
  implicit val ioRuntime: IORuntime = IORuntime.global
  private val singleThreadCtx: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  private val logger = new WriterTLogger[IO].mapK(λ[FunctionK[W, F]](ReaderT.liftF(_))).withContext

  checkAll(
    "ContContextLogger",
    LoggerTests[F](logger, reader => reader.run(Map()).written.evalOn(singleThreadCtx).unsafeRunSync()).all
  )

  it should "pick up context from F[_]" in {
    forAll { (loggerMessage: LoggerMessage, ctx: Map[String, String]) =>
      val List(written) = logger.log(loggerMessage).apply(ctx).written.unsafeRunSync()
      written.context shouldBe loggerMessage.context ++ ctx
    }
  }

  it should "embed context in all messages" in {
    forAll { (msgs: List[LoggerMessage], ctx: Map[String, String]) =>
      val written = logger.log(msgs).apply(ctx).written.unsafeRunSync()
      written.map(_.context) shouldBe msgs.map(_.context ++ ctx)
    }
  }
}
