package io.odin.loggers

import cats.effect.concurrent.Ref
import cats.instances.list._
import cats.syntax.all._
import io.odin.{Logger, LoggerMessage, OdinSpec}
import monix.catnap.ConcurrentQueue
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

import scala.concurrent.duration._

class AsyncLoggerSpec extends OdinSpec {

  implicit private val scheduler: TestScheduler = TestScheduler()

  case class RefLogger(ref: Ref[Task, List[LoggerMessage]]) extends DefaultLogger[Task] {
    def log(msg: LoggerMessage): Task[Unit] = {
      //have to run, otherwise ref never gets updated somehow
      Task.pure(ref.update(_ :+ msg).runSyncUnsafe())
    }
  }

  it should "push logs down the chain" in {
    forAll { msgs: List[LoggerMessage] =>
      (for {
        ref <- Ref.of[Task, List[LoggerMessage]](List.empty)
        logger <- AsyncLogger.withAsync[Task]()(RefLogger(ref))
        _ <- msgs.traverse(logger.log)
        _ = scheduler.tick(10.millis)
        reported <- ref.get
      } yield {
        reported shouldBe msgs
      }).runSyncUnsafe()
    }
  }

  it should "push logs to the queue" in {
    forAll { msgs: List[LoggerMessage] =>
      (for {
        queue <- ConcurrentQueue.unbounded[Task, LoggerMessage]()
        logger = AsyncLogger(queue)(Logger.noop[Task])
        _ <- msgs.traverse(logger.log)
        reported <- queue.drain(0, Int.MaxValue)
      } yield {
        reported shouldBe msgs
      }).runSyncUnsafe()
    }
  }

}
