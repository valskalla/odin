package io.odin.loggers

import java.io.{ByteArrayOutputStream, PrintStream}

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._
import io.odin.Level._
import io.odin.formatter.Formatter
import io.odin.{Level, LoggerMessage, OdinSpec}

class ConsoleLoggerSpec extends OdinSpec {

  implicit private val ioRuntime: IORuntime = IORuntime.global

  it should "route all messages with level <= INFO to stdout" in {
    forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
      whenever(loggerMessage.level <= Info) {
        val outBaos = new ByteArrayOutputStream()
        val stdOut = new PrintStream(outBaos)
        val errBaos = new ByteArrayOutputStream()
        val stdErr = new PrintStream(errBaos)

        val consoleLogger = ConsoleLogger[IO](formatter, stdOut, stdErr, Level.Trace)
        consoleLogger.log(loggerMessage).unsafeRunSync()
        outBaos.toString() shouldBe (formatter.format(loggerMessage) + System.lineSeparator())
      }
    }
  }

  it should "route all messages with level >= WARN to stderr" in {
    forAll { (loggerMessage: LoggerMessage, formatter: Formatter) =>
      whenever(loggerMessage.level > Info) {
        val outBaos = new ByteArrayOutputStream()
        val stdOut = new PrintStream(outBaos)
        val errBaos = new ByteArrayOutputStream()
        val stdErr = new PrintStream(errBaos)

        val consoleLogger = ConsoleLogger[IO](formatter, stdOut, stdErr, Level.Trace)
        consoleLogger.log(loggerMessage).unsafeRunSync()
        errBaos.toString() shouldBe (formatter.format(loggerMessage) + System.lineSeparator())
      }
    }
  }
}
