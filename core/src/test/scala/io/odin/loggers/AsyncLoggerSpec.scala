package io.odin.loggers

import cats.effect.Resource
import cats.effect.concurrent.Ref
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}
import monix.catnap.ConcurrentQueue
import monix.eval.Task
import monix.execution.schedulers.TestScheduler
import io.odin.syntax._

import scala.concurrent.duration._

class AsyncLoggerSpec extends OdinSpec {
  implicit private val scheduler: TestScheduler = TestScheduler()

  case class RefLogger(ref: Ref[Task, List[LoggerMessage]], override val minLevel: Level = Level.Trace) extends DefaultLogger[Task](minLevel) {
    def submit(msg: LoggerMessage): Task[Unit] = Task.raiseError(new IllegalStateException("Async should always batch"))

    override def submit(msgs: List[LoggerMessage]): Task[Unit] = {
      ref.update(_ ::: msgs)
    }

    def withMinimalLevel(level: Level): Logger[Task] = copy(minLevel = level)
  }

  it should "push logs down the chain" in {
    forAll { msgs: List[LoggerMessage] =>
      (for {
        ref <- Resource.liftF(Ref.of[Task, List[LoggerMessage]](List.empty))
        logger <- RefLogger(ref).withMinimalLevel(Level.Trace).withAsync()
        _ <- Resource.liftF(msgs.traverse(logger.log))
        _ = scheduler.tick(10.millis)
        reported <- Resource.liftF(ref.get)
      } yield {
        reported shouldBe msgs
      }).use(Task(_)).runSyncUnsafe()
    }
  }

  it should "push logs to the queue" in {
    forAll { msgs: List[LoggerMessage] =>
      (for {
        queue <- ConcurrentQueue.unbounded[Task, LoggerMessage]()
        logger = AsyncLogger(queue, 1.millis, Logger.noop[Task]).withMinimalLevel(Level.Trace)
        _ <- msgs.traverse(logger.log)
        reported <- queue.drain(0, Int.MaxValue)
      } yield {
        reported shouldBe msgs
      }).runSyncUnsafe()
    }
  }

  it should "ignore errors in underlying logger" in {
    val errorLogger = new DefaultLogger[Task](Level.Trace) {
      def submit(msg: LoggerMessage): Task[Unit] = Task.raiseError(new Error)

      def withMinimalLevel(level: Level): Logger[Task] = this
    }
    forAll { msgs: List[LoggerMessage] =>
      (for {
        queue <- ConcurrentQueue.unbounded[Task, LoggerMessage]()
        logger = AsyncLogger(queue, 1.millis, errorLogger)
        _ <- logger.log(msgs)
        result <- logger.drain
      } yield {
        result shouldBe (())
      }).runSyncUnsafe()
    }
  }
}
