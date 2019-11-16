package io.odin

import io.odin.meta.Position
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait OdinSpec extends FlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  val nonEmptyStringGen: Gen[String] = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)

  val levelGen: Gen[Level] = Gen.oneOf(Level.Trace, Level.Debug, Level.Info, Level.Warn, Level.Error)
  implicit val levelArbitrary: Arbitrary[Level] = Arbitrary(levelGen)

  val positionGen: Gen[Position] = for {
    fileName <- nonEmptyStringGen
    enclosureName <- nonEmptyStringGen
    packageName <- nonEmptyStringGen
    line <- Gen.posNum[Int]
  } yield {
    Position(fileName, enclosureName, packageName, line)
  }
  implicit val positionArbitrary: Arbitrary[Position] = Arbitrary(positionGen)

  val loggerMessageGen: Gen[LoggerMessage] = for {
    level <- levelGen
    msg <- Gen.alphaNumStr
    context <- Arbitrary.arbitrary[Map[String, String]]
    exception <- Gen.option(Arbitrary.arbitrary[Throwable])
    position <- positionGen
    threadName <- nonEmptyStringGen
    timestamp <- Gen.choose(0, System.currentTimeMillis())
  } yield {
    LoggerMessage(
      level = level,
      message = () => msg,
      context = context,
      exception = exception,
      position = position,
      threadName = threadName,
      timestamp = timestamp
    )
  }
  implicit val loggerMessageArbitrary: Arbitrary[LoggerMessage] = Arbitrary(loggerMessageGen)

}
