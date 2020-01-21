package io.odin.formatter

import io.odin.OdinSpec
import io.odin.formatter.options.ThrowableFormat
import io.odin.formatter.options.ThrowableFormat.{Depth, Indent}
import io.odin.formatter.FormatterSpec.TestException
import org.scalacheck.Gen

import scala.util.control.NoStackTrace

class FormatterSpec extends OdinSpec {

  behavior of "Formatter.formatThrowable with ThrowableFormat"

  it should "support Indent" in forAll(indentGen) { indent =>
    val indentStr = indent match {
      case Indent.NoIndent    => ""
      case Indent.Fixed(size) => "".padTo(size, ' ')
    }

    val error1 = TestException("Exception 1")
    val error2 = new RuntimeException("Exception 2", error1)

    val format = ThrowableFormat(Depth.Full, indent)
    val result = Formatter.formatThrowable(error2, format)
    val lines = result.split(System.lineSeparator()).toList

    val (cause, trace) = lines.partition(_.startsWith("Caused by"))

    val expectedCausedBy = List(
      "Caused by: java.lang.RuntimeException: Exception 2",
      s"Caused by: io.odin.formatter.FormatterSpec$$TestException: Exception 1"
    )

    cause shouldBe expectedCausedBy
    trace.forall(_.startsWith(indentStr)) shouldBe true
  }

  it should "support Depth" in forAll(depthGen) { depth =>
    val indent = Indent.Fixed(2)

    val depthSize = depth match {
      case Depth.Full        => None
      case Depth.Fixed(size) => Some(size)
    }

    val error1 = TestException("Exception 1")
    val error2 = new RuntimeException("Exception 2", error1)

    val format = ThrowableFormat(depth, indent)
    val result = Formatter.formatThrowable(error2, format)
    val lines = result.split(System.lineSeparator()).toList

    val (cause, trace) = lines.partition(_.startsWith("Caused by"))

    val expectedCausedBy = List(
      "Caused by: java.lang.RuntimeException: Exception 2",
      s"Caused by: io.odin.formatter.FormatterSpec$$TestException: Exception 1"
    )

    val expectedLength = {
      val stackTraceLength = error2.getStackTrace.length
      depthSize.fold(stackTraceLength)(size => stackTraceLength.min(size))
    }

    cause shouldBe expectedCausedBy
    trace.length shouldBe expectedLength
  }

  private lazy val indentGen: Gen[Indent] =
    Gen.oneOf(
      Gen.const(Indent.NoIndent),
      Gen.posNum[Int].filter(_ > 0).map(size => Indent.Fixed(size))
    )

  private lazy val depthGen: Gen[Depth] =
    Gen.oneOf(
      Gen.const(Depth.Full),
      Gen.posNum[Int].map(size => Depth.Fixed(size))
    )

}

object FormatterSpec {

  final case class TestException(message: String) extends Throwable(message) with NoStackTrace

}
