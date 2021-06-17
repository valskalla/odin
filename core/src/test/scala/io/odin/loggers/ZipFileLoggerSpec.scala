package io.odin.loggers

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import java.util.zip.{ZipEntry, ZipInputStream}

import cats.effect.Resource
import io.odin.util.ListDirectory
import io.odin.{Level, OdinSpec}
import monix.eval.Task
import org.scalatest.Assertion

import scala.concurrent.duration._

class ZipFileLoggerSpec extends OdinSpec {

  private val fileResource = Resource.make[Task, Path] {
    Task.delay(Files.createTempDirectory(UUID.randomUUID().toString))
  } { file =>
    Task.delay {
      ListDirectory(file).filter(_.isFile).foreach(_.delete())
      Files.delete(file)
    }
  }

  {
    import monix.execution.Scheduler.Implicits.global
    it should "parse ZipNamePattern correctly" in {

      def matches(pattern: String)(prefix: String, dateFormat: String, suffix: String, dateLength: Int): Assertion = {
        val parsedOpt = ZipperProvider
          .parse[Task](pattern)
          .runSyncUnsafe()
          .map(parsed =>
            assert(
              parsed.prefix == prefix && parsed.dateFormat == dateFormat && parsed.suffix == suffix && parsed.dateLength == dateLength
            )
          )
        parsedOpt.fold(assert(false))(a => a)
      }

      val p1 = "output.%d{yyyy-MM-dd'T'HH:mm:ss.SSS}.log.zip"
      matches(p1)("output.", "yyyy-MM-dd'T'HH:mm:ss.SSS", ".log.zip", 23)

      val p2 = "%d{yyyy-MM-dd'T'HH:mm:ss.SSS}.log.zip"
      matches(p2)("", "yyyy-MM-dd'T'HH:mm:ss.SSS", ".log.zip", 23)

      val p3 = "output.%d{yyyy-MM-dd'T'HH:mm:ss.SSS}"
      matches(p3)("output.", "yyyy-MM-dd'T'HH:mm:ss.SSS", "", 23)
    }

    it should "write formatted message into file" in {
      (for {
        path <- fileResource
        logFilePath = s"${path.toString}/output.log"
        logger <- ZipFileLogger[Task](
          logFilePath,
          "output.%d{yyyy-MM-dd'T'HH:mm:ss.SSS}.log.zip",
          10.minutes,
          keepSize = 3,
          minLevel = Level.Trace
        )
        _ <- Resource.liftF(logger.info("hello"))
      } yield {
        val logFile = Paths.get(logFilePath)
        val content = new String(Files.readAllBytes(logFile))
        assert(content.contains("hello"))
      }).use(Task(_))
        .runSyncUnsafe()
    }

    it should "write formatted messages into file" in {
      (for {
        path <- fileResource
        logFilePath = s"${path.toString}/output.log"
        logger <- ZipFileLogger[Task](
          logFilePath,
          "output.%d{yyyy-MM-dd'T'HH:mm:ss.SSS}.log.zip",
          10.minutes,
          keepSize = 3,
          minLevel = Level.Trace
        )
        _ <- Resource.liftF(logger.info("hello"))
        _ <- Resource.liftF(logger.info("hello2"))
      } yield {
        val logFile = ListDirectory(path).filter(_.isFile).head.toPath
        val array = new String(Files.readAllBytes(logFile)).split(lineSeparator)
        assert(array(0).contains("hello") && array(1).contains("hello2"))
      }).use(Task(_))
        .runSyncUnsafe()
    }

    it should "cleanup the log file" in {
      (for {
        path <- fileResource
        logFilePath = s"${path.toString}/output.log"
        logger <- ZipFileLogger[Task](
          logFilePath,
          "output.%d{yyyy-MM-dd'T'HH:mm:ss.SSS}.log.zip",
          50.millis,
          keepSize = 3,
          minLevel = Level.Trace
        )
        _ <- Resource.liftF(logger.info("hello"))
        _ <- Resource.liftF(Task.sleep(200.millis))
      } yield {
        val logFile = Paths.get(logFilePath)
        new String(Files.readAllBytes(logFile)) shouldBe ""
      }).use(Task(_))
        .runSyncUnsafe()
    }

    it should "produce a predictable number of zips" in {
      (for {
        path <- fileResource
        logFilePath = s"${path.toString}/output.log"
        logger <- ZipFileLogger[Task](
          logFilePath,
          "output.%d{yyyy-MM-dd'T'HH:mm:ss.SSS}.log.zip",
          50.millis,
          keepSize = 2,
          minLevel = Level.Trace
        )
        _ <- Resource.liftF(logger.info("hello"))
        _ <- Resource.liftF(Task.sleep(700.millis))
        fileZip = path.toFile.listFiles().toList.filter(_.getName.endsWith(".zip")).head
        unzippedFile <- getFirstUnzippedFile(fileZip)
      } yield {
        val size = path.toFile.listFiles().length
        assert(3 <= size && size <= 4 && unzippedFile.getName == "output.log")
      }).use(Task(_)).runSyncUnsafe()
    }

    def getFirstUnzippedFile(fileZip: File): Resource[Task, ZipEntry] =
      for {
        fis <- Resource.fromAutoCloseable(Task.delay(new FileInputStream(fileZip)))
        zis <- Resource.fromAutoCloseable(Task.delay(new ZipInputStream(fis)))
      } yield zis.getNextEntry
  }
}
