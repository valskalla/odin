package io.odin

import java.util.concurrent.Executors

import cats.Eq
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import io.odin.formatter.Formatter
import org.scalacheck.Arbitrary

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

trait EqInstances {
  @tailrec
  final def retrySample[T](implicit arb: Arbitrary[T]): T = arb.arbitrary.sample match {
    case Some(v) => v
    case _       => retrySample[T]
  }

  implicit def loggerEq[F[_]](
      implicit arbitraryString: Arbitrary[String],
      arbitraryCtx: Arbitrary[Map[String, String]],
      arbitraryThrowable: Arbitrary[Throwable],
      eqF: Eq[F[Unit]]
  ): Eq[Logger[F]] =
    (x: Logger[F], y: Logger[F]) => {
      val msg = retrySample[String]
      val ctx = retrySample[Map[String, String]]
      val throwable = retrySample[Throwable]
      eqF.eqv(x.trace(msg), y.trace(msg)) &&
      eqF.eqv(x.trace(msg, throwable), y.trace(msg, throwable)) &&
      eqF.eqv(x.trace(msg, ctx), y.trace(msg, ctx)) &&
      eqF.eqv(x.trace(msg, ctx, throwable), y.trace(msg, ctx, throwable)) &&
      eqF.eqv(x.debug(msg), y.debug(msg)) &&
      eqF.eqv(x.debug(msg, throwable), y.debug(msg, throwable)) &&
      eqF.eqv(x.debug(msg, ctx), y.debug(msg, ctx)) &&
      eqF.eqv(x.debug(msg, ctx, throwable), y.debug(msg, ctx, throwable)) &&
      eqF.eqv(x.info(msg), y.info(msg)) &&
      eqF.eqv(x.info(msg, throwable), y.info(msg, throwable)) &&
      eqF.eqv(x.info(msg, ctx), y.info(msg, ctx)) &&
      eqF.eqv(x.info(msg, ctx, throwable), y.info(msg, ctx, throwable)) &&
      eqF.eqv(x.warn(msg), y.warn(msg)) &&
      eqF.eqv(x.warn(msg, throwable), y.warn(msg, throwable)) &&
      eqF.eqv(x.warn(msg, ctx), y.warn(msg, ctx)) &&
      eqF.eqv(x.warn(msg, ctx, throwable), y.warn(msg, ctx, throwable)) &&
      eqF.eqv(x.error(msg), y.error(msg)) &&
      eqF.eqv(x.error(msg, throwable), y.error(msg, throwable)) &&
      eqF.eqv(x.error(msg, ctx), y.error(msg, ctx)) &&
      eqF.eqv(x.error(msg, ctx, throwable), y.error(msg, ctx, throwable))
    }

  /**
    * IO evaluates effects on different threads more often. Therefore `loggerMessageEq` returns `false` due to different threads.
    * Evaluating effects on a single thread prevents such an issue.
    */
  implicit def eqIO[A](implicit eqA: Eq[A], ioRuntime: IORuntime): Eq[IO[A]] = Eq.instance { (ioA, ioB) =>
    eqA.eqv(ioA.evalOn(singleThreadCtx).unsafeRunSync(), ioB.evalOn(singleThreadCtx).unsafeRunSync())
  }

  implicit val loggerMessageEq: Eq[LoggerMessage] = Eq.instance { (lm1, lm2) =>
    val LoggerMessage(lvl1, msg1, context1, exception1, position1, threadName1, timestamp1) = lm1
    val LoggerMessage(lvl2, msg2, context2, exception2, position2, threadName2, timestamp2) = lm2
    lvl1 == lvl2 &&
    msg1.value == msg2.value &&
    context1 == context2 &&
    exception1 == exception2 &&
    position1 == position2 &&
    threadName1 == threadName2 &&
    timestamp1 == timestamp2
  }

  implicit def formatterEq(implicit arbLoggerMsg: Arbitrary[LoggerMessage]): Eq[Formatter] =
    Eq.instance { (fmt1, fmt2) =>
      val msg = retrySample[LoggerMessage]
      fmt1.format(msg) == fmt2.format(msg)
    }

  private val singleThreadCtx: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
}
