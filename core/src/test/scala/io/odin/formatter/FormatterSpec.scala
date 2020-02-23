package io.odin.formatter

import io.odin.{LoggerMessage, OdinSpec}
import io.odin.formatter.options.{PositionFormat, ThrowableFormat}
import io.odin.formatter.options.ThrowableFormat.{Depth, Indent}
import io.odin.formatter.FormatterSpec.TestException
import io.odin.meta.Position
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

  behavior of "Formatter.formatPosition with PositionFormat"

  it should "support PositionFormat" in forAll(positionFormatGen, Gen.posNum[Int]) { (format, line) =>
    val position = Position("file.scala", "io.odin.formatter.Formatter enclosingMethod", "", line)

    val expected = format match {
      case PositionFormat.Full              => s"${position.enclosureName}:$line"
      case PositionFormat.AbbreviatePackage => s"i.o.f.Formatter enclosingMethod:$line"
    }

    val result = Formatter.formatPosition(position, format)

    result shouldBe expected
  }

  it should "not abbreviate empty package" in {
    val position = Position("file.scala", "enclosingMethod", "", -1)

    val full = Formatter.formatPosition(position, PositionFormat.Full)
    val abbreviated = Formatter.formatPosition(position, PositionFormat.AbbreviatePackage)

    full shouldBe "enclosingMethod"
    abbreviated shouldBe "enclosingMethod"
  }

  it should "print context by default" in {
    forAll { (msg: LoggerMessage) =>
      whenever(msg.context.nonEmpty) {
        val formatted = Formatter.default.format(msg)
        val lookup = msg.context
          .map {
            case (key, value) => s"$key: $value"
          }
          .mkString(", ")
        formatted should include(lookup)
      }
    }
  }

  it should "disable context print if `printCtx=false`" in {
    forAll { (msg: LoggerMessage) =>
      val formatted = Formatter
        .create(ThrowableFormat.Default, PositionFormat.Full, colorful = false, printCtx = false)
        .format(msg)
      val lookup = msg.context
        .map {
          case (key, value) => s"$key: $value"
        }
        .mkString(", ")
      formatted should not include lookup
    }
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

  private lazy val positionFormatGen: Gen[PositionFormat] =
    Gen.oneOf(
      Gen.const(PositionFormat.Full),
      Gen.const(PositionFormat.AbbreviatePackage)
    )

}

object FormatterSpec {

  final case class TestException(message: String) extends Throwable(message) with NoStackTrace

}
