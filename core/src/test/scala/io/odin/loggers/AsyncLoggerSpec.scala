package io.odin.loggers

import cats.effect.std.Queue
import cats.effect.testkit.{TestContext, TestInstances}
import cats.effect.{IO, Outcome, Ref, Resource}
import cats.syntax.all._
import io.odin.syntax._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}

import scala.concurrent.duration._

class AsyncLoggerSpec extends OdinSpec with TestInstances {
  private implicit val ticker: Ticker = Ticker(TestContext())

  case class RefLogger(ref: Ref[IO, List[LoggerMessage]], override val minLevel: Level = Level.Trace)
      extends DefaultLogger[IO](minLevel) {
    def submit(msg: LoggerMessage): IO[Unit] = IO.raiseError(new IllegalStateException("Async should always batch"))

    override def submit(msgs: List[LoggerMessage]): IO[Unit] = {
      ref.update(_ ::: msgs)
    }

    def withMinimalLevel(level: Level): Logger[IO] = copy(minLevel = level)
  }

  it should "push logs down the chain" in {
    forAll { msgs: List[LoggerMessage] =>
      val io =
        for {
          ref <- Resource.eval(Ref.of[IO, List[LoggerMessage]](List.empty))
          logger <- RefLogger(ref).withMinimalLevel(Level.Trace).withAsync()
          _ <- Resource.eval(msgs.traverse(logger.log))
          _ = ticker.ctx.tick(10.millis)
          reported <- Resource.eval(ref.get)
        } yield reported

      unsafeRun(io.use(IO(_))) shouldBe Outcome.succeeded(Some(msgs))
    }
  }

  it should "push logs to the queue" in {
    forAll { msgs: List[LoggerMessage] =>
      val io =
        for {
          queue <- Queue.unbounded[IO, LoggerMessage]
          logger = AsyncLogger(queue, 1.millis, Logger.noop[IO]).withMinimalLevel(Level.Trace)
          _ <- msgs.traverse(logger.log)
          reported <- List.fill(msgs.length)(queue.take).sequence
        } yield reported

      unsafeRun(io) shouldBe Outcome.succeeded(Some(msgs))
    }
  }

  it should "ignore errors in underlying logger" in {
    val errorLogger = new DefaultLogger[IO](Level.Trace) {
      def submit(msg: LoggerMessage): IO[Unit] = IO.raiseError(new Error)

      def withMinimalLevel(level: Level): Logger[IO] = this
    }
    forAll { msgs: List[LoggerMessage] =>
      val io =
        for {
          queue <- Queue.unbounded[IO, LoggerMessage]
          logger = AsyncLogger(queue, 1.millis, errorLogger)
          _ <- logger.log(msgs)
          result <- logger.drain
        } yield result

      unsafeRun(io) shouldBe Outcome.succeeded(Some(()))
    }
  }
}
