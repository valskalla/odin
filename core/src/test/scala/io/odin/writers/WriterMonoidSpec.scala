package io.odin.writers

import java.util.UUID

import cats.data.WriterT
import cats.effect.IO
import cats.instances.list._
import cats.instances.tuple._
import cats.instances.uuid._
import cats.instances.unit._
import cats.syntax.all._
import cats.kernel.laws.discipline.MonoidTests
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}
import org.scalacheck.{Arbitrary, Gen}

class WriterMonoidSpec extends OdinSpec {
  type F[A] = WriterT[IO, List[(LoggerMessage, Formatter, UUID)], A]

  implicit def arbitraryWriter: Arbitrary[LogWriter[F]] =
    Arbitrary(Gen.uuid.map(NamedLogWriter))

  case class NamedLogWriter(writerId: UUID) extends LogWriter[F] {
    def write(msg: LoggerMessage, formatter: Formatter): F[Unit] =
      WriterT.tell(List((msg, formatter, writerId)))
  }

  checkAll("LogWriter", MonoidTests[LogWriter[F]].monoid)

  it should "(writer1 |+| writer2).write <-> writer1.write |+| writer2.write" in {
    forAll { (uuid1: UUID, uuid2: UUID, msg: LoggerMessage, fmt: Formatter) =>
      val writer1: LogWriter[F] = NamedLogWriter(uuid1)
      val writer2: LogWriter[F] = NamedLogWriter(uuid2)
      val a = (writer1 |+| writer2).write(msg, fmt).written
      val b = (writer1.write(msg, fmt) |+| writer2.write(msg, fmt)).written
      a.unsafeRunSync() shouldBe b.unsafeRunSync()
    }
  }
}
