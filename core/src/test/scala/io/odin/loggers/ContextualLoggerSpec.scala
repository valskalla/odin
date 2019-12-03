package io.odin.loggers

import cats.arrow.FunctionK
import cats.data.{ReaderT, WriterT}
import cats.effect.{Clock, IO, Timer}
import cats.instances.list._
import cats.mtl.instances.all._
import io.odin.syntax._
import io.odin.{LoggerMessage, OdinSpec}

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

class ContextualLoggerSpec extends OdinSpec {
  type W[A] = WriterT[IO, List[LoggerMessage], A]
  type F[A] = ReaderT[W, Map[String, String], A]

  implicit val hasContext: HasContext[Map[String, String]] = (env: Map[String, String]) => env
  implicit val timer: Timer[IO] = new Timer[IO] {
    def clock: Clock[IO] = new Clock[IO] {
      def realTime(unit: TimeUnit): IO[Long] = IO.pure(0)

      def monotonic(unit: TimeUnit): IO[Long] = IO.pure(0)
    }

    def sleep(duration: FiniteDuration): IO[Unit] = ???
  }

  private val logger = new WriterTLogger[IO].mapK(λ[FunctionK[W, F]](ReaderT.liftF(_))).withContext

  checkAll("ContContextLogger", LoggerTests[F](logger, reader => reader.run(Map()).written.unsafeRunSync()).all)

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
