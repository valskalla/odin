package io.odin.loggers

import java.util.UUID

import cats.data.WriterT
import cats.effect.{Clock, IO, Timer}
import cats.instances.list._
import cats.instances.tuple._
import cats.instances.unit._
import cats.instances.uuid._
import cats.kernel.laws.discipline.MonoidTests
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}
import org.scalacheck.{Arbitrary, Gen}

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

class LoggerMonoidSpec extends OdinSpec {
  type F[A] = WriterT[IO, List[(UUID, LoggerMessage)], A]

  checkAll("Logger", MonoidTests[Logger[F]].monoid)

  it should "(logger1 |+| logger2).log <-> (logger1.log |+| logger2.log)" in {
    forAll { (uuid1: UUID, uuid2: UUID, msg: LoggerMessage) =>
      val logger1: Logger[F] = NamedLogger(uuid1)
      val logger2: Logger[F] = NamedLogger(uuid2)
      val a = (logger1 |+| logger2).log(msg)
      val b = logger1.log(msg) |+| logger2.log(msg)
      a.written.unsafeRunSync() shouldBe b.written.unsafeRunSync()
    }
  }

  it should "(logger1 |+| logger2).log(list) <-> (logger1.log |+| logger2.log(list))" in {
    forAll { (uuid1: UUID, uuid2: UUID, msg: List[LoggerMessage]) =>
      val logger1: Logger[F] = NamedLogger(uuid1)
      val logger2: Logger[F] = NamedLogger(uuid2)
      val a = (logger1 |+| logger2).log(msg)
      val b = logger1.log(msg) |+| logger2.log(msg)
      a.written.unsafeRunSync() shouldBe b.written.unsafeRunSync()
    }
  }

  it should "set minimal level for underlying loggers" in {
    forAll { (uuid1: UUID, uuid2: UUID, level: Level, msg: List[LoggerMessage]) =>
      val logger1: Logger[F] = NamedLogger(uuid1)
      val logger2: Logger[F] = NamedLogger(uuid2)
      val a = (logger1 |+| logger2).withMinimalLevel(level).log(msg)
      val b = (logger1.withMinimalLevel(level) |+| logger2.withMinimalLevel(level)).log(msg)
      a.written.unsafeRunSync() shouldBe b.written.unsafeRunSync()
    }
  }

  case class NamedLogger(loggerId: UUID) extends DefaultLogger[F] {
    def log(msg: LoggerMessage): F[Unit] = WriterT.tell(List(loggerId -> msg))
  }

  implicit def timer: Timer[IO] = new Timer[IO] {
    def clock: Clock[IO] = new Clock[IO] {
      def realTime(unit: TimeUnit): IO[Long] = IO.pure(0)

      def monotonic(unit: TimeUnit): IO[Long] = IO.pure(0)
    }

    def sleep(duration: FiniteDuration): IO[Unit] = ???
  }

  implicit def arbitraryWriterLogger: Arbitrary[Logger[F]] = Arbitrary(
    Gen.uuid.map(NamedLogger)
  )
}
