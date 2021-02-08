package io.odin.loggers

import java.util.UUID

import cats.data.WriterT
import cats.effect.{Clock, IO}
import cats.kernel.laws.discipline.MonoidTests
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}
import org.scalacheck.{Arbitrary, Gen}

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

  it should "respect underlying log level of each logger" in {
    forAll { (uuid1: UUID, uuid2: UUID, level1: Level, level2: Level, msg: List[LoggerMessage]) =>
      whenever(level1 != level2) {
        val logger1: Logger[F] = NamedLogger(uuid1).withMinimalLevel(level1)
        val logger2: Logger[F] = NamedLogger(uuid2).withMinimalLevel(level2)
        val logs = msg.map(_.copy(level = level2))
        val written = (logger1 |+| logger2).log(logs).written.unsafeRunSync()

        if (level1 > level2) {
          written shouldBe logs.tupleLeft(uuid2)
        } else {
          written shouldBe logs.tupleLeft(uuid1) ++ logs.tupleLeft(uuid2)
        }
      }
    }
  }

  case class NamedLogger(loggerId: UUID, override val minLevel: Level = Level.Trace) extends DefaultLogger[F](minLevel) {
    def submit(msg: LoggerMessage): F[Unit] = WriterT.tell(List(loggerId -> msg))
    def withMinimalLevel(level: Level): Logger[F] = copy(minLevel = level)
  }

  implicit def clock: Clock[IO] = zeroClock

  implicit def arbitraryWriterLogger: Arbitrary[Logger[F]] = Arbitrary(
    Gen.uuid.map(NamedLogger(_))
  )
}
