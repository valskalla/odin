package io.odin.loggers

import cats.laws.discipline._
import cats.{Eq, Monad}
import io.odin.{Level, Logger, LoggerMessage}
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait LoggerTests[F[_]] extends Laws {
  def loggerLaws: LoggerLaws[F]
  def logger: Logger[F]

  def all(implicit
      arbMsg: Arbitrary[LoggerMessage],
      arbLvl: Arbitrary[Level],
      eqF: Eq[List[LoggerMessage]]
  ): RuleSet = new SimpleRuleSet(
    "logger",
    "checks minLevel" -> Prop.forAll((msg: LoggerMessage, level: Level) =>
      loggerLaws.checksMinLevel(logger, msg, level)
    ),
    "log(list) <-> list.traverse(log)" -> Prop.forAll((msgs: List[LoggerMessage]) =>
      loggerLaws.batchEqualsToTraverse(logger, msgs)
    )
  )
}

object LoggerTests {
  def apply[F[_]](l: Logger[F], extract: F[Unit] => List[LoggerMessage])(implicit
      monad: Monad[F]
  ): LoggerTests[F] = new LoggerTests[F] {
    def loggerLaws: LoggerLaws[F] = new LoggerLaws[F] {
      val F: Monad[F] = monad
      val written: F[Unit] => List[LoggerMessage] = extract
    }

    def logger: Logger[F] = l
  }
}
