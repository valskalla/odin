package io.odin

import java.time.LocalDateTime
import cats.effect.Clock
import cats.{Applicative, Eval}
import io.odin.formatter.Formatter
import io.odin.meta.Position
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.{Checkers, ScalaCheckDrivenPropertyChecks}
import org.typelevel.discipline.Laws

import scala.concurrent.duration._

trait OdinSpec extends AnyFlatSpec with Matchers with Checkers with ScalaCheckDrivenPropertyChecks with EqInstances {
  def checkAll(name: String, ruleSet: Laws#RuleSet): Unit = {
    for ((id, prop) <- ruleSet.all.properties)
      it should (name + "." + id) in {
        check(prop)
      }
  }

  def zeroClock[F[_]: Applicative]: Clock[F] = fixedClock(0)

  def fixedClock[F[_]](time: Long)(implicit F: Applicative[F]): Clock[F] = new Clock[F] {
    def applicative: Applicative[F] = F
    def monotonic: F[FiniteDuration] = F.pure(time.millis)
    def realTime: F[FiniteDuration] = F.pure(time.millis)
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
      context <- Gen.mapOfN(20, nonEmptyStringGen.flatMap(key => nonEmptyStringGen.map(key -> _)))
      exception <- Gen.option(Arbitrary.arbitrary[Throwable])
      position <- positionGen
      threadName <- nonEmptyStringGen
      timestamp <- Gen.choose(0L, startTime)
    } yield {
      LoggerMessage(
        level = level,
        message = Eval.now(msg),
        context = context,
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

  val formatterGen: Gen[Formatter] = Gen.oneOf(Formatter.default, Formatter.colorful)
  implicit val formatterArbitrary: Arbitrary[Formatter] = Arbitrary(formatterGen)

  val localDateTimeGen: Gen[LocalDateTime] = for {
    year <- Gen.choose(0, LocalDateTime.now().getYear)
    month <- Gen.choose(1, 12)
    day <- Gen.choose(1, 28)
    hour <- Gen.choose(0, 23)
    minute <- Gen.choose(0, 59)
    second <- Gen.choose(0, 59)
  } yield {
    LocalDateTime.of(year, month, day, hour, minute, second)
  }
  implicit val localDateTimeArbitrary: Arbitrary[LocalDateTime] = Arbitrary(localDateTimeGen)
}
