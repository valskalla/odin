package io.odin.extras.loggers

import cats.data.Kleisli
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.mtl.instances.all._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.order._
import io.odin.extras.syntax._
import io.odin.loggers.DefaultLogger
import io.odin.loggers.HasContext
import io.odin.syntax._
import io.odin.Level
import io.odin.LoggerMessage
import io.odin.OdinSpec
import monix.eval.Task
import monix.execution.schedulers.TestScheduler

class ConditionalLoggerSpec extends OdinSpec {

  implicit private val scheduler: TestScheduler = TestScheduler()

  type F[A] = Kleisli[Task, Map[String, String], A]

  case class RefLogger(ref: Ref[F, List[LoggerMessage]]) extends DefaultLogger[F] {
    def log(msg: LoggerMessage): F[Unit] = ref.update(_ :+ msg)
  }

  implicit private val hasContext: HasContext[Map[String, String]] = (env: Map[String, String]) => env

  it should "use log level of the inner logger in case of success" in {
    forAll { (messages: List[LoggerMessage], ctx: Map[String, String]) =>
      val fa =
        for {
          ref <- Ref.of[F, List[LoggerMessage]](List.empty)

          _ <- RefLogger(ref)
            .withMinimalLevel(Level.Info)
            .withContext
            .withErrorLevel(Level.Debug)(logger => logger.log(messages))

          written <- ref.get
        } yield written

      val written = fa.run(ctx)
      val expected = messages.filter(_.level >= Level.Info).map(m => m.copy(context = m.context ++ ctx))

      written.map(_ shouldBe expected).runToFuture(implicitly)
    }
  }

  it should "use log level of the conditional logger in case of error" in {
    forAll { (messages: List[LoggerMessage], ctx: Map[String, String]) =>
      val error = new RuntimeException("Boom")

      val fa =
        for {
          ref <- Ref.of[F, List[LoggerMessage]](List.empty)

          attempt <- RefLogger(ref)
            .withMinimalLevel(Level.Info)
            .withContext
            .withErrorLevel(Level.Debug)(logger => logger.log(messages) >> Sync[F].raiseError[Unit](error))
            .attempt

          written <- ref.get
        } yield (attempt, written)

      fa.run(ctx)
        .map {
          case (attempt, written) =>
            val expected = messages.filter(_.level >= Level.Debug).map(m => m.copy(context = m.context ++ ctx))

            attempt shouldBe Left(error)
            written shouldBe expected
        }
        .runToFuture(implicitly)

    }
  }

}
