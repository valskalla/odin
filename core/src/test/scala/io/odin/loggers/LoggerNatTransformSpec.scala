package io.odin.loggers

import cats.data.{Writer, WriterT}
import cats.effect.{Clock, IO}
import cats.{~>, Id}
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}

class LoggerNatTransformSpec extends OdinSpec {
  type F[A] = Writer[List[LoggerMessage], A]
  type FF[A] = WriterT[IO, List[LoggerMessage], A]

  it should "transform each method" in {
    forAll { (msg: String, ctx: Map[String, String], throwable: Throwable, timestamp: Long) =>
      implicit val clk: Clock[Id] = fixedClock(timestamp)
      val logF = logger.withMinimalLevel(Level.Trace)
      val logFF = logF.mapK(nat).withMinimalLevel(Level.Trace)
      check(logF.trace(msg), logFF.trace(msg))
      check(logF.trace(msg, throwable), logFF.trace(msg, throwable))
      check(logF.trace(msg, ctx), logFF.trace(msg, ctx))
      check(logF.trace(msg, ctx, throwable), logFF.trace(msg, ctx, throwable))

      check(logF.debug(msg), logFF.debug(msg))
      check(logF.debug(msg, throwable), logFF.debug(msg, throwable))
      check(logF.debug(msg, ctx), logFF.debug(msg, ctx))
      check(logF.debug(msg, ctx, throwable), logFF.debug(msg, ctx, throwable))

      check(logF.info(msg), logFF.info(msg))
      check(logF.info(msg, throwable), logFF.info(msg, throwable))
      check(logF.info(msg, ctx), logFF.info(msg, ctx))
      check(logF.info(msg, ctx, throwable), logFF.info(msg, ctx, throwable))

      check(logF.warn(msg), logFF.warn(msg))
      check(logF.warn(msg, throwable), logFF.warn(msg, throwable))
      check(logF.warn(msg, ctx), logFF.warn(msg, ctx))
      check(logF.warn(msg, ctx, throwable), logFF.warn(msg, ctx, throwable))

      check(logF.error(msg), logFF.error(msg))
      check(logF.error(msg, throwable), logFF.error(msg, throwable))
      check(logF.error(msg, ctx), logFF.error(msg, ctx))
      check(logF.error(msg, ctx, throwable), logFF.error(msg, ctx, throwable))
    }
  }

  private val nat: F ~> FF = new (F ~> FF) {
    private val idToIo = new (Id ~> IO) {
      def apply[A](fa: Id[A]): IO[A] = IO.pure(fa)
    }

    def apply[A](fa: F[A]): FF[A] =
      fa.mapK(idToIo)
  }

  private def logger(implicit clock: Clock[Id]): Logger[F] = new WriterTLogger[Id]

  private def check(fnF: => F[Unit], fnFF: => FF[Unit]) = {
    val List(loggerMessageF) = fnF.written
    val List(loggerMessageFF) = fnFF.written.unsafeRunSync()
    loggerMessageEq.eqv(loggerMessageF, loggerMessageFF) shouldBe true
  }
}
