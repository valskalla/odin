package io.odin.writers

import java.io.{ByteArrayOutputStream, PrintStream}

import cats.effect.IO
import io.odin.formatter.Formatter
import io.odin.{LoggerMessage, OdinSpec}

class StdLogWriterSpec extends OdinSpec {

  System.setProperty("file.encoding", "UTF-8")

  it should "write formatted log to PrintStream" in {
    forAll { loggerMessage: LoggerMessage =>
      val baos = new ByteArrayOutputStream()
      val ps = new PrintStream(baos)
      StdLogWriter.mk[IO](ps).write(loggerMessage, Formatter.simple).unsafeRunSync()
      val str = new String(baos.toByteArray)
      str shouldBe Formatter.simple.format(loggerMessage) + lineSeparator
      ps.close()
    }
  }

}
