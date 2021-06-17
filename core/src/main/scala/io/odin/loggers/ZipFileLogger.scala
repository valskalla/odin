package io.odin.loggers

import java.io.{BufferedWriter, File, FileInputStream, FileOutputStream, PrintWriter}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import java.util.concurrent.TimeUnit
import java.util.zip.{ZipEntry, ZipOutputStream}

import cats.Monad
import cats.data.OptionT
import cats.effect.concurrent.{Ref, Semaphore}
import cats.effect.{Concurrent, ContextShift, Resource, Sync, Timer}
import cats.effect.implicits._
import cats.implicits._
import io.odin.{Level, Logger, LoggerMessage}
import io.odin.formatter.Formatter
import io.odin.loggers.ZipperProvider.ParsedZipNamePattern

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Try

object ZipFileLogger {

  def apply[F[_]: Timer: ContextShift](
      outputFilePath: String,
      zipNamePattern: String,
      rolloverInterval: FiniteDuration,
      keepSize: Int,
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  )(implicit F: Concurrent[F]): Resource[F, Logger[F]] = {

    def mk: F[Ref[F, BufferedWriter]] = Ref.of[F, BufferedWriter](initBufferedWriter(Paths.get(outputFilePath)))

    def close(sem: BiSemaphore[F])(bufferRef: Ref[F, BufferedWriter]): F[Unit] = {
      sem.acquireSingle >> (for {
        buffer <- bufferRef.get
        _ <- F.delay(buffer.close())
      } yield ()).guarantee(sem.releaseSingle)
    }

    for {
      sem <- Resource.liftF(BiSemaphore[F])
      bufferRef <- Resource.make(mk)(close(sem))
      _ <- Zipper.zipper(outputFilePath, zipNamePattern, rolloverInterval, keepSize, bufferRef, sem)
    } yield ZipFileLogger(sem, bufferRef, formatter, minLevel)
  }

  def initBufferedWriter(path: Path): BufferedWriter =
    if (path.toFile.exists())
      Files.newBufferedWriter(path, StandardOpenOption.APPEND)
    else
      Files.newBufferedWriter(path)
}

case class ZipFileLogger[F[_]: Timer](
    sem: BiSemaphore[F],
    bufferRef: Ref[F, BufferedWriter],
    formatter: Formatter,
    override val minLevel: Level
)(implicit F: Sync[F])
    extends DefaultLogger[F](minLevel) {

  override def log(msg: LoggerMessage): F[Unit] = {
    sem.acquireConcurrent >> (for {
      buffer <- bufferRef.get
      _ <- F.delay(buffer.write(formatter.format(msg) + System.lineSeparator()))
      _ <- F.delay(buffer.flush())
    } yield ()).guarantee(sem.releaseConcurrent)
  }
}

object Zipper {

  def zipper[F[_]: Timer](
      outputFilePath: String,
      zipNamePattern: String,
      rolloverInterval: FiniteDuration,
      keepSize: Int,
      bufferRef: Ref[F, BufferedWriter],
      sem: BiSemaphore[F]
  )(implicit F: Concurrent[F], cs: ContextShift[F]): Resource[F, Unit] = {

    def zipperJob(parsedOpt: Option[ParsedZipNamePattern]): F[Unit] =
      sem.acquireSingle >> (for {
        buffer <- bufferRef.get
        _ <- F.delay(buffer.close())
        outputFile <- F.delay(new File(outputFilePath))
        // delete older zips that exceed keep size
        _ <- ZipperProvider.cleanupJob(parsedOpt, outputFile, keepSize)
        // zip outputFile
        nextZipName <- ZipperProvider.nextZipName(zipNamePattern, parsedOpt)
        zipFile <- F.delay(new File(outputFile.getParentFile.getAbsolutePath + File.separator + nextZipName))
        _ <- ZipperProvider.zip(outputFile, zipFile)
        // erase outputFile content
        _ <- F.delay(new PrintWriter(outputFile).close())
        _ <- bufferRef.set(ZipFileLogger.initBufferedWriter(outputFile.toPath))
      } yield ()).guarantee(sem.releaseSingle)

    def zipperLoopRec(parsedOpt: Option[ParsedZipNamePattern], start: Long): F[Unit] =
      for {
        _ <- Timer[F].sleep(100.millis)
        _ <- cs.shift
        now <- ZipperProvider.nowF
        _ <- if (now - start >= rolloverInterval.toMillis) zipperJob(parsedOpt) >> zipperLoopRec(parsedOpt, now)
        else zipperLoopRec(parsedOpt, start)
      } yield ()

    def zipperLoop: F[Unit] =
      for {
        parsedOpt <- ZipperProvider.parse(zipNamePattern)
        now <- ZipperProvider.nowF
        _ <- zipperLoopRec(parsedOpt, now)
      } yield ()

    Resource.make(F.start(zipperLoop))(_.cancel).as(())
  }
}

object ZipperProvider {
  val logbackBeginDate: String = "%d{"
  val logbackEndDate: String = "}"
  val timeZone: TimeZone = TimeZone.getTimeZone("UTC")

  def nowF[F[_]: Timer]: F[Long] = Timer[F].clock.realTime(TimeUnit.MILLISECONDS)

  def nowFormattedF[F[_]: Timer: Sync](formatter: SimpleDateFormat): F[Option[String]] =
    nowF.flatMap(now => Sync[F].delay(Try(formatter.format(new Date(now))).toOption))

  def formatterF[F[_]: Sync](dateFormat: String): F[Option[SimpleDateFormat]] = Sync[F].delay {
    Try {
      val df = new SimpleDateFormat(dateFormat)
      df.setTimeZone(timeZone)
      df
    }.toOption
  }

  case class ParsedZipNamePattern(
      prefix: String,
      dateFormat: String,
      suffix: String,
      dateLength: Int,
      sdf: SimpleDateFormat
  )
  def parse[F[_]: Timer: Sync](zipNamePattern: String): F[Option[ParsedZipNamePattern]] = {

    val tuple: F[Option[(String, String, String)]] = Sync[F].delay(Try {
      val beginIdx = zipNamePattern.indexOf(logbackBeginDate)
      val endIdx = zipNamePattern.indexOf(logbackEndDate, beginIdx)
      val prefix = zipNamePattern.substring(0, beginIdx)
      val dateFormat = zipNamePattern.substring(beginIdx + logbackBeginDate.length, endIdx)
      val suffix = zipNamePattern.substring(endIdx + logbackEndDate.length)
      Some((prefix, dateFormat, suffix))
    }.fold(_ => None, o => o))

    val optT: OptionT[F, ParsedZipNamePattern] = for {
      (prefix, dateFormat, suffix) <- OptionT(tuple)
      sdf <- OptionT(formatterF(dateFormat))
      date <- OptionT(nowFormattedF(sdf))
    } yield ParsedZipNamePattern(prefix, dateFormat, suffix, date.length, sdf)
    optT.value
  }

  def extractDate(candidate: String, parsed: ParsedZipNamePattern): Option[Date] =
    Try {
      val leftPart = candidate.substring(0, parsed.prefix.length)
      val datePart = candidate.substring(parsed.prefix.length, parsed.prefix.length + parsed.dateLength)
      val rightPart = candidate.substring(parsed.prefix.length + parsed.dateLength)

      if (leftPart.equals(parsed.prefix) && rightPart.equals(parsed.suffix)) {
        Some(parsed.sdf.parse(datePart))
      } else
        None
    }.fold(_ => None, o => o)

  case class FileDate(f: File, date: Date)
  def cleanupJob[F[_]: Timer](
      parsedOpt: Option[ParsedZipNamePattern],
      outputFile: File,
      keepSize: Int
  )(implicit F: Sync[F]): F[Unit] =
    parsedOpt.fold(F.unit)(parsed =>
      F.delay {
        val filtered: List[FileDate] = outputFile.getParentFile.listFiles.toList
          .foldLeft(List[FileDate]())((acc, f) => {
            val dateOpt: Option[Date] = extractDate(f.getName, parsed)
            dateOpt.fold(acc)(d => FileDate(f, d) :: acc)
          })
          .sortWith((a, b) => a.date.compareTo(b.date) > 0)
        val nDelete = filtered.length - keepSize

        if (nDelete > 0)
          filtered.takeRight(nDelete).foreach(_.f.delete())
      }
    )

  def nextZipName[F[_]: Sync: Timer](
      zipNamePattern: String,
      parsed: Option[ParsedZipNamePattern]
  ): F[String] =
    parsed.fold(Sync[F].pure(zipNamePattern)) { parsed =>
      val dateOptF: F[Option[String]] = nowFormattedF(parsed.sdf)
      dateOptF.map(_.fold(zipNamePattern)(date => s"${parsed.prefix}$date${parsed.suffix}"))
    }

  def zip[F[_]](from: File, to: File)(implicit F: Sync[F]): F[Unit] = {
    @tailrec
    def write(fis: FileInputStream, bytes: Array[Byte], zipOut: ZipOutputStream): Unit = {
      val length = fis.read(bytes)
      if (length >= 0) {
        zipOut.write(bytes, 0, length)
        write(fis, bytes, zipOut)
      }
    }

    val res: Resource[F, Unit] = for {
      fos <- Resource.fromAutoCloseable(F.delay(new FileOutputStream(to)))
      zipOut <- Resource.fromAutoCloseable(F.delay(new ZipOutputStream(fos)))
      fis <- Resource.fromAutoCloseable(F.delay(new FileInputStream(from)))
      _ <- Resource.liftF(F.delay(zipOut.putNextEntry(new ZipEntry(from.getName))))
      bytes = Array.ofDim[Byte](1024)
      _ <- Resource.liftF(F.delay(write(fis, bytes, zipOut)))
    } yield ()
    res.use(_ => F.unit)
  }
}

// bi semaphore for threads of type A and B.
// threads of type A and B can't happen concurrently.
trait BiSemaphore[F[_]] {
  // multiple threads of type A can acquire without blocking between them
  def acquireConcurrent: F[Unit]
  def releaseConcurrent: F[Unit]
  // only a single thread of type B can acquire
  def acquireSingle: F[Unit]
  def releaseSingle: F[Unit]
}

object BiSemaphore {
  def apply[F[_]](implicit F: Concurrent[F]): F[BiSemaphore[F]] =
    for {
      semConc <- Semaphore[F](1)
      semSing <- Semaphore[F](1)
      refConc <- Ref.of[F, Int](0)
      biSemaphore = of[F](semConc, semSing, refConc)
    } yield biSemaphore

  def of[F[_]](semConc: Semaphore[F], semSing: Semaphore[F], refConc: Ref[F, Int])(
      implicit F: Monad[F]
  ): BiSemaphore[F] = new BiSemaphore[F] {
    override def acquireConcurrent: F[Unit] =
      for {
        _ <- semConc.acquire
        num <- refConc.get
        _ <- F.whenA(num == 0)(semSing.acquire)
        _ <- refConc.update(_ + 1)
        _ <- semConc.release
      } yield ()

    override def releaseConcurrent: F[Unit] =
      for {
        _ <- semConc.acquire
        _ <- refConc.update(_ - 1)
        num <- refConc.get
        _ <- F.whenA(num == 0)(semSing.release)
        _ <- semConc.release
      } yield ()

    override def acquireSingle: F[Unit] = semSing.acquire

    override def releaseSingle: F[Unit] = semSing.release
  }
}
