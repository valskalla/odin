package io.odin.config

import cats.data.WriterT
import cats.effect.{IO, Timer}
import cats.syntax.all._
import io.odin.loggers.DefaultLogger
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}

class ConfigSpec extends OdinSpec {
  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)

  type F[A] = WriterT[IO, List[(String, LoggerMessage)], A]

  case class TestLogger(loggerName: String) extends DefaultLogger[F] {
    def log(msg: LoggerMessage): F[Unit] = WriterT.tell(List(loggerName -> msg))
  }

  it should "route based on the package" in {
    forAll { ls: List[LoggerMessage] =>
      val withEnclosure = ls.groupBy(_.position.enclosureName)
      val routerLogger =
        enclosureRouting[F](
          withEnclosure.toList.map {
            case (key, _) => key -> TestLogger(key)
          }: _*
        ).withNoopFallback.withMinimalLevel(Level.Trace)

      val written = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written shouldBe ls.map(msg => msg.position.enclosureName -> msg)
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "route based on the class" in {
    forAll(nonEmptyStringGen, nonEmptyStringGen, nonEmptyStringGen) {
      (msg: String, loggerName1: String, loggerName2: String) =>
        val routerLogger =
          classRouting[F](
            classOf[ConfigSpec] -> TestLogger(loggerName1),
            classOf[TestClass[F]] -> TestLogger(loggerName2)
          ).withNoopFallback

        val List((ln1, _)) = routerLogger.info(msg).written.unsafeRunSync()
        val List((ln2, _)) = (new TestClass[F](routerLogger)).log(msg).written.unsafeRunSync()

        ln1 shouldBe loggerName1
        ln2 shouldBe loggerName2
    }
  }

  it should "route based on the level" in {
    forAll { (ls: List[LoggerMessage]) =>
      val withLevels = ls.groupBy(_.level)
      val routerLogger = levelRouting[F](
        withLevels.map {
          case (key, _) => key -> TestLogger(key.show)
        }
      ).withNoopFallback.withMinimalLevel(Level.Trace)

      val written = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written.toMap shouldBe ls.map(msg => msg.level.show -> msg).toMap
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "fallback to provided logger" in {
    forAll { ls: List[LoggerMessage] =>
      val fallback = TestLogger("fallback")
      val routerLogger = enclosureRouting[F]().withFallback(fallback).withMinimalLevel(Level.Trace)

      val written = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written shouldBe ls.map(msg => "fallback" -> msg)
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "check underlying min level" in {
    forAll { (minLevel: Level, ls: List[LoggerMessage]) =>
      val underlying = TestLogger("underlying").withMinimalLevel(minLevel)
      val logger = enclosureRouting("" -> underlying).withNoopFallback

      val written = ls.traverse(logger.log).written.unsafeRunSync().map(_._2)
      val batchWritten = logger.log(ls).written.unsafeRunSync().map(_._2)

      written shouldBe ls.filter(_.level >= minLevel)
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "check fallback min level" in {
    forAll { (minLevel: Level, ls: List[LoggerMessage]) =>
      val fallback = TestLogger("fallback").withMinimalLevel(minLevel)
      val logger = enclosureRouting[F]().withFallback(fallback)

      val written = ls.traverse(logger.log).written.unsafeRunSync().map(_._2)
      val batchWritten = logger.log(ls).written.unsafeRunSync().map(_._2)

      written shouldBe ls.filter(_.level >= minLevel)
      batchWritten should contain theSameElementsAs written
    }
  }
}

class TestClass[F[_]](logger: Logger[F]) {
  def log(msg: String): F[Unit] = logger.info(msg)
}
