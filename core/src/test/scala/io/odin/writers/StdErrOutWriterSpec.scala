package io.odin.writers

import java.io.{ByteArrayOutputStream, PrintStream}

import cats.effect.{ContextShift, IO}
import cats.instances.list._
import cats.syntax.all._
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}

class StdErrOutWriterSpec extends OdinSpec {

  implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  it should "write to stdout" in {
    forAll { (msgs: List[LoggerMessage], fmt: Formatter) =>
      val baos = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(baos)) {

        val writer = StdOutLogWriter[IO]
        msgs.traverse(writer.write(_, fmt)).unsafeRunSync()
        baos.toString shouldBe msgs
          .map(fmt.format)
          .mkString(System.lineSeparator()) + (if (msgs.nonEmpty)
                                                 System.lineSeparator()
                                               else "")
      }
    }
  }

  it should "write to stderr" in {
    forAll { (msgs: List[LoggerMessage], fmt: Formatter) =>
      val baos = new ByteArrayOutputStream()
      Console.withErr(new PrintStream(baos)) {

        val writer = StdErrLogWriter[IO]
        msgs.traverse(writer.write(_, fmt)).unsafeRunSync()
        baos.toString shouldBe msgs
          .map(fmt.format)
          .mkString(System.lineSeparator()) + (if (msgs.nonEmpty)
                                                 System.lineSeparator()
                                               else "")
      }
    }
  }

}
