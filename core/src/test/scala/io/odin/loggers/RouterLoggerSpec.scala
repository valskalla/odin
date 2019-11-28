package io.odin.loggers

import cats.data.WriterT
import cats.effect.{IO, Timer}
import cats.instances.list._
import cats.syntax.all._
import io.odin.syntax._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}

class RouterLoggerSpec extends OdinSpec {
  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)

  type F[A] = WriterT[IO, List[(String, LoggerMessage)], A]

  case class TestLogger(loggerName: String) extends DefaultLogger[F] {
    def log(msg: LoggerMessage): F[Unit] = WriterT.tell(List(loggerName -> msg))
  }

  it should "route based on the package" in {
    forAll { ls: List[LoggerMessage] =>
      val withEnclosure = ls.groupBy(_.position.enclosureName)
      val routerLogger = RouterLogger
        .packageRoutingLogger[F](
          withEnclosure.toList.map {
            case (key, _) => key -> TestLogger(key)
          }: _*
        )
        .withNoopFallback

      val written = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written shouldBe ls.map(msg => msg.position.enclosureName -> msg)
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "route based on the class" in {
    forAll(nonEmptyStringGen, nonEmptyStringGen, nonEmptyStringGen) {
      (msg: String, loggerName1: String, loggerName2: String) =>
        val routerLogger = RouterLogger
          .classRoutingLogger[F](
            classOf[RouterLoggerSpec] -> TestLogger(loggerName1),
            classOf[TestClass[F]] -> TestLogger(loggerName2)
          )
          .withNoopFallback

        val List((ln1, _)) = routerLogger.info(msg).written.unsafeRunSync()
        val List((ln2, _)) = (new TestClass[F](routerLogger)).log(msg).written.unsafeRunSync()

        ln1 shouldBe loggerName1
        ln2 shouldBe loggerName2
    }
  }

  it should "route based on the level" in {
    forAll { (ls: List[LoggerMessage]) =>
      val withLevels = ls.groupBy(_.level)
      val routerLogger = RouterLogger
        .levelRoutingLogger[F](
          withLevels.map {
            case (key, _) => key -> TestLogger(key.show)
          }
        )
        .withNoopFallback

      val written = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written.toMap shouldBe ls.map(msg => msg.level.show -> msg).toMap
      batchWritten should contain theSameElementsAs written
    }
  }

  it should "noop logs with level less than set" in {
    val logger = new WriterTLogger[IO]

    forAll { (level: Level, msgs: List[LoggerMessage]) =>
      val log = logger.withMinimalLevel(level)
      val written = msgs.traverse(log.log).written.unsafeRunSync()
      val batchWritten = log.log(msgs).written.unsafeRunSync()
      written shouldBe msgs.filter(_.level >= level)
      batchWritten shouldBe written
    }
  }

  it should "fallback to provided logger" in {
    forAll { ls: List[LoggerMessage] =>
      val fallback = TestLogger("fallback")
      val routerLogger = RouterLogger
        .packageRoutingLogger[F]()
        .withFallback(fallback)

      val written = ls.traverse(routerLogger.log).written.unsafeRunSync()
      val batchWritten = routerLogger.log(ls).written.unsafeRunSync()

      written shouldBe ls.map(msg => "fallback" -> msg)
      batchWritten should contain theSameElementsAs written
    }
  }
}

class TestClass[F[_]](logger: Logger[F]) {
  def log(msg: String): F[Unit] = logger.info(msg)
}
