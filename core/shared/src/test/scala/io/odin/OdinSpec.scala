package io.odin

import cats.Applicative
import cats.Eval
import cats.effect.Clock
import cats.effect.Timer
import io.odin.formatter.Formatter
import io.odin.meta.Position
import org.scalacheck.Arbitrary
import org.scalacheck.Cogen
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.Checkers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.typelevel.discipline.Laws

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.TimeUnit

trait OdinSpec extends AnyFlatSpec with Matchers with Checkers with ScalaCheckDrivenPropertyChecks with EqInstances {

  def checkAll(name: String, ruleSet: Laws#RuleSet): Unit = {
    for ((id, prop) <- ruleSet.all.properties)
      it should (name + "." + id) in {
        check(prop)
      }
  }

  def zeroTimer[F[_]](implicit F: Applicative[F]): Timer[F] = new Timer[F] {
    def clock: Clock[F] = new Clock[F] {
      def realTime(unit: TimeUnit): F[Long] = F.pure(0L)

      def monotonic(unit: TimeUnit): F[Long] = F.pure(0L)
    }

    def sleep(duration: FiniteDuration): F[Unit] = ???
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
      timestamp <- Gen.choose(0, startTime)
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

}
