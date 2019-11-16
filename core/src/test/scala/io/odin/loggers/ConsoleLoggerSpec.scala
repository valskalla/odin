package io.odin.loggers

import cats.data.WriterT
import cats.effect.{IO, Timer}
import cats.instances.list._
import io.odin.formatter.Formatter
import io.odin.writers.LogWriter
import io.odin.{Level, LoggerMessage, OdinSpec}

class ConsoleLoggerSpec extends OdinSpec {

  private val out = "out"
  private val err = "err"
  type F[A] = WriterT[IO, List[(String, LoggerMessage)], A]

  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)

  private val stdOutWriter = new LogWriter[F] {
    def write(msg: LoggerMessage, formatter: Formatter): F[Unit] = WriterT.tell(List((out, msg)))
  }

  private val stdErrWriter = new LogWriter[F] {
    def write(msg: LoggerMessage, formatter: Formatter): F[Unit] = WriterT.tell(List((err, msg)))
  }

  private val consoleLogger = ConsoleLogger[F](Formatter.simple, stdOutWriter, stdErrWriter)

  it should "route all messages with level <= INFO to stdout" in {
    forAll { loggerMessage: LoggerMessage =>
      whenever(loggerMessage.level.value <= Level.Info.value) {
        val List((o, log)) = consoleLogger.log(loggerMessage).written.unsafeRunSync()
        o shouldBe out
        log shouldBe loggerMessage
      }
    }
  }

  it should "route all messages with level >= WARN to stderr" in {
    forAll { loggerMessage: LoggerMessage =>
      whenever(loggerMessage.level.value >= Level.Warn.value) {
        val List((o, log)) = consoleLogger.log(loggerMessage).written.unsafeRunSync()
        o shouldBe err
        log shouldBe loggerMessage
      }
    }
  }

}
