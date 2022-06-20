package io.odin.loggers

import cats.effect.std.{Queue, Semaphore}
import cats.effect.unsafe.IORuntime
import cats.effect.{IO, Ref, Resource}
import cats.syntax.all._
import io.odin.syntax._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}

import scala.concurrent.duration._

class AsyncLoggerSpec extends OdinSpec {
  import AsyncLoggerSpec._

  implicit private val ioRuntime: IORuntime = IORuntime.global

  case class RefLogger(ref: Ref[IO, List[LoggerMessage]], override val minLevel: Level = Level.Trace)
      extends DefaultLogger[IO](minLevel) {
    def submit(msg: LoggerMessage): IO[Unit] =
      ref.update(_ :+ msg)

    override def submit(msgs: List[LoggerMessage]): IO[Unit] = {
      ref.update(_ ::: msgs)
    }

    def withMinimalLevel(level: Level): Logger[IO] = copy(minLevel = level)
  }

  it should "push logs down the chain" in {
    forAll { (msgs: List[LoggerMessage]) =>
      (for {
        ref <- Resource.eval(Ref.of[IO, List[LoggerMessage]](List.empty))
        logger <- RefLogger(ref).withMinimalLevel(Level.Trace).withAsync()
        _ <- Resource.eval(msgs.traverse(logger.log))
        _ <- Resource.eval(IO.sleep(10.millis))
        reported <- Resource.eval(ref.get)
      } yield {
        reported shouldBe msgs
      }).use(IO(_)).unsafeRunSync()
    }
  }

  it should "push logs to the queue" in {
    forAll { (msgs: List[LoggerMessage]) =>
      (for {
        queueLogger <- createQueueLogger(Level.Trace)
        _ <- queueLogger.withAsync(1.millis).use(logger => msgs.traverse(logger.log))
        reported <- queueLogger.take(msgs.size)
      } yield {
        reported shouldBe msgs
      }).unsafeRunSync()
    }
  }

  it should "ignore errors in underlying logger" in {
    val errorLogger = new DefaultLogger[IO](Level.Trace) {
      def submit(msg: LoggerMessage): IO[Unit] = IO.raiseError(new Error)

      def withMinimalLevel(level: Level): Logger[IO] = this
    }
    forAll { (msgs: List[LoggerMessage]) =>
      (for {
        queue <- Queue.unbounded[IO, IO[Unit]]
        sem <- Semaphore[IO](1)
        logger = AsyncLogger(queue, sem, 1.millis, errorLogger)
        _ <- logger.log(msgs)
        result <- logger.drain
      } yield {
        result shouldBe (())
      }).unsafeRunSync()
    }
  }

  it should "respect updated minimal level" in {
    forAll { (msgs: List[LoggerMessage]) =>
      (for {
        queueLogger <- createQueueLogger(Level.Info)
        infoMessages <- IO.pure(msgs.filter(_.level >= Level.Info))
        _ <- queueLogger.withAsync(timeWindow = 1.millis).use { logger =>
          val traceLogger = logger.withMinimalLevel(Level.Trace)

          for {
            _ <- msgs.traverse(logger.log) // only Info, Warn, Error messages should be logged
            _ <- msgs.traverse(traceLogger.log) // all messages should be logged
          } yield ()
        }
        info <- queueLogger.take(infoMessages.size)
        reported <- queueLogger.take(msgs.size)
      } yield {
        info shouldBe infoMessages
        reported shouldBe msgs
      }).unsafeRunSync()
    }
  }

  private def createQueueLogger(minLevel: Level): IO[QueueLogger] =
    for {
      queue <- Queue.unbounded[IO, LoggerMessage]
    } yield QueueLogger(queue, minLevel)
}

object AsyncLoggerSpec {

  case class QueueLogger(queue: Queue[IO, LoggerMessage], level: Level) extends DefaultLogger[IO](level) {
    def submit(msg: LoggerMessage): IO[Unit] =
      queue.offer(msg)

    def withMinimalLevel(level: Level): Logger[IO] =
      copy(level = level)

    def take(count: Int): IO[List[LoggerMessage]] =
      queue.take.replicateA(count)
  }

}
