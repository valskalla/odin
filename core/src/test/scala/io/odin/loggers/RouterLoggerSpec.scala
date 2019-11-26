package io.odin.loggers

import cats.data.WriterT
import cats.effect.{IO, Timer}
import cats.instances.list._
import cats.syntax.all._
import io.odin.{Level, Logger, LoggerMessage, OdinSpec}
import io.odin.syntax._

class RouterLoggerSpec extends OdinSpec {
  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)

  type F[A] = WriterT[IO, List[(String, LoggerMessage)], A]

  case class TestLogger(loggerName: String) extends DefaultLogger[F] {
    def log(msg: LoggerMessage): F[Unit] = WriterT.tell(List(loggerName -> msg))
  }

  it should "route logs to corresponding loggers" in {
    forAll { (p1: (String, LoggerMessage), p2: (String, LoggerMessage)) =>
      val routerLogger = RouterLogger[F] {
        case msg if msg == p1._2 => TestLogger(p1._1)
        case msg if msg == p2._2 => TestLogger(p2._1)
      }

      val List(written1) = routerLogger.log(p1._2).written.unsafeRunSync()
      val List(written2) = routerLogger.log(p2._2).written.unsafeRunSync()

      written1 shouldBe p1
      written2 shouldBe p2
    }
  }

  it should "route based on the package" in {
    forAll { (p1: (String, LoggerMessage), p2: (String, LoggerMessage)) =>
      val routerLogger = RouterLogger.packageRoutingLogger[F](
        p1._2.position.packageName -> TestLogger(p1._1),
        p2._2.position.packageName -> TestLogger(p2._1)
      )

      val List(written1) = routerLogger.log(p1._2).written.unsafeRunSync()
      val List(written2) = routerLogger.log(p2._2).written.unsafeRunSync()

      written1 shouldBe p1
      written2 shouldBe p2
    }
  }

  it should "route based on the class" in {
    forAll { (msg: String, loggerName1: String, loggerName2: String) =>
      val routerLogger = RouterLogger.classRoutingLogger[F](
        classOf[RouterLoggerSpec] -> TestLogger(loggerName1),
        classOf[TestClass[F]] -> TestLogger(loggerName2)
      )

      val List((ln1, _)) = routerLogger.info(msg).written.unsafeRunSync()
      val List((ln2, _)) = (new TestClass[F](routerLogger)).log(msg).written.unsafeRunSync()

      ln1 shouldBe loggerName1
      ln2 shouldBe loggerName2
    }
  }

  it should "noop logs with level less than set" in {
    val logger = new WriterTLogger[IO]

    forAll { (level: Level, msg: LoggerMessage) =>
      val log = logger.withMinimalLevel(level)
      val written = log.log(msg).written.unsafeRunSync()
      if (msg.level >= level) {
        written shouldBe List(msg)
      } else {
        written shouldBe Nil
      }
    }
  }
}

class TestClass[F[_]](logger: Logger[F]) {
  def log(msg: String): F[Unit] = logger.info(msg)
}
