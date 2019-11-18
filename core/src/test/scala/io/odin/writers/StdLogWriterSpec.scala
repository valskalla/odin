package io.odin.writers

import java.io.{ByteArrayOutputStream, PrintStream}

import cats.effect.IO
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}

class StdLogWriterSpec extends OdinSpec {

  it should "write formatted log to PrintStream" in {
    forAll { loggerMessage: LoggerMessage =>
      val baos = new ByteArrayOutputStream()
      val ps = new PrintStream(baos)
      StdLogWriter.mk[IO](ps).write(loggerMessage, Formatter.default).unsafeRunSync()
      val str = new String(baos.toByteArray)
      str shouldBe Formatter.default.format(loggerMessage) + lineSeparator
      ps.close()
    }
  }

}
