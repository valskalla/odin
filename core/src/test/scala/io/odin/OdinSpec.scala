package io.odin

import cats.Eval
import io.odin.formatter.Formatter
import io.odin.meta.Position
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.{Checkers, ScalaCheckDrivenPropertyChecks}
import org.typelevel.discipline.Laws

trait OdinSpec extends FlatSpec with Matchers with Checkers with ScalaCheckDrivenPropertyChecks with EqInstances {
  def checkAll(name: String, ruleSet: Laws#RuleSet): Unit = {
    for ((id, prop) <- ruleSet.all.properties)
      it should (name + "." + id) in {
        check(prop)
      }
  }

  val lineSeparator: String = System.lineSeparator()

  val nonEmptyStringGen: Gen[String] = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)

  val levelGen: Gen[Level] = Gen.oneOf(Level.Trace, Level.Debug, Level.Info, Level.Warn, Level.Error)
  implicit val levelArbitrary: Arbitrary[Level] = Arbitrary(levelGen)

  val positionGen: Gen[Position] = for {
    fileName <- nonEmptyStringGen
    enclosureName <- Gen.uuid.map(_.toString)
    packageName <- nonEmptyStringGen
    line <- Gen.posNum[Int]
  } yield {
    Position(fileName, enclosureName, packageName, line)
  }
  implicit val positionArbitrary: Arbitrary[Position] = Arbitrary(positionGen)

  val loggerMessageGen: Gen[LoggerMessage] = {
    val startTime = System.currentTimeMillis()
    for {
      level <- levelGen
      msg <- Gen.alphaNumStr
      context <- Gen.listOfN(20, Gen.alphaNumStr.flatMap(key => Gen.alphaNumStr.map(key -> _)))
      exception <- Gen.option(Arbitrary.arbitrary[Throwable])
      position <- positionGen
      threadName <- nonEmptyStringGen
      timestamp <- Gen.choose(0, startTime)
    } yield {
      LoggerMessage(
        level = level,
        message = Eval.now(msg),
        context = context.toMap,
        exception = exception,
        position = position,
        threadName = threadName,
        timestamp = timestamp
      )
    }
  }
  implicit val loggerMessageArbitrary: Arbitrary[LoggerMessage] = Arbitrary(loggerMessageGen)

  implicit val cogenLoggerMessage: Cogen[LoggerMessage] =
    Cogen[LoggerMessage]((msg: LoggerMessage) => msg.level.hashCode().toLong + msg.message.value.hashCode().toLong)

  val formatterGen: Gen[Formatter] = Gen.const(Formatter.default)
  implicit val formatterArbitrary: Arbitrary[Formatter] = Arbitrary(formatterGen)
}
