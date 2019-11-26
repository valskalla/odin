package io.odin

import java.nio.file.{Files, Paths}
import java.util.UUID
import java.util.concurrent.TimeUnit

import cats.effect.{ContextShift, IO, Timer}
import io.odin.loggers.DefaultLogger
import io.odin.syntax._
import io.odin.formatter.Formatter
import io.odin.json.{Formatter => JsonFormatter}
import io.odin.meta.Position
import org.openjdk.jmh.annotations._

// $COVERAGE-OFF$
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(warmups = 2, jvmArgsAppend = Array("-XX:MaxInlineLevel=18", "-XX:MaxInlineSize=270", "-XX:MaxTrivialSize=12"))
abstract class OdinBenchmarks {
  val message: String = "msg"
  val context: Map[String, String] = Map("hello" -> "world")
  val throwable = new Error()
  val loggerMessage: LoggerMessage = LoggerMessage(
    io.odin.Level.Debug,
    () => message,
    context,
    Some(throwable),
    Position(
      "foobar",
      "foo/bar/foobar.scala",
      "io.odin.foobar",
      100
    ),
    "just-a-test-thread",
    1574716305L
  )

  implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)
  implicit val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)
}

@State(Scope.Benchmark)
class DefaultLoggerBenchmarks extends OdinBenchmarks {
  val noop: Logger[IO] = Logger.noop

  val defaultLogger: Logger[IO] = new DefaultLogger[IO] {
    def log(msg: LoggerMessage): IO[Unit] = noop.log(msg)
  }

  @Benchmark
  def msg(): Unit = defaultLogger.info(message).unsafeRunSync()

  @Benchmark
  def msgAndCtx(): Unit = defaultLogger.info(message, context).unsafeRunSync()

  @Benchmark
  def msgCtxThrowable(): Unit = defaultLogger.info(message, context, throwable).unsafeRunSync()
}

@State(Scope.Benchmark)
class FileLoggerBenchmarks extends OdinBenchmarks {
  val fileName: String = Files.createTempFile(UUID.randomUUID().toString, "").toAbsolutePath.toString
  val (logger: Logger[IO], cancelToken: IO[Unit]) =
    fileLogger[IO](fileName).allocated.unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(1000)
  def msg(): Unit =
    for (_ <- 1 to 1000) logger.info(message).unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(1000)
  def msgAndCtx(): Unit =
    for (_ <- 1 to 1000) logger.info(message, context).unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(1000)
  def msgCtxThrowable(): Unit =
    for (_ <- 1 to 1000) logger.info(message, context, throwable).unsafeRunSync()

  @TearDown
  def tearDown(): Unit = {
    cancelToken.unsafeRunSync()
    Files.delete(Paths.get(fileName))
  }
}

@State(Scope.Benchmark)
class AsyncLoggerBenchmark extends OdinBenchmarks {
  val fileName: String = Files.createTempFile(UUID.randomUUID().toString, "").toAbsolutePath.toString
  val (asyncLogger: Logger[IO], cancelToken: IO[Unit]) =
    fileLogger[IO](fileName).withAsync(maxBufferSize = Some(1000000)).allocated.unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(1000)
  def msg(): Unit = for (_ <- 1 to 1000) asyncLogger.info(message).unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(1000)
  def msgAndCtx(): Unit =
    for (_ <- 1 to 1000) asyncLogger.info(message, context).unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(1000)
  def msgCtxThrowable(): Unit =
    for (_ <- 1 to 1000) asyncLogger.info(message, context, throwable).unsafeRunSync()

  @TearDown
  def tearDown(): Unit = {
    cancelToken.unsafeRunSync()
    Files.delete(Paths.get(fileName))
  }
}

@State(Scope.Benchmark)
class RouterLoggerBenchmarks extends OdinBenchmarks {
  val fileName: String = Files.createTempFile(UUID.randomUUID().toString, "").toAbsolutePath.toString

  val (routerLogger: Logger[IO], cancelToken: IO[Unit]) =
    fileLogger[IO](fileName).withMinimalLevel(io.odin.Level.Info).allocated.unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(1000)
  def info(): Unit = for (_ <- 1 to 1000) routerLogger.info(message).unsafeRunSync()

  @Benchmark
  @OperationsPerInvocation(1000)
  def trace(): Unit =
    for (_ <- 1 to 1000) routerLogger.trace(message).unsafeRunSync()

  @TearDown
  def tearDown(): Unit = {
    cancelToken.unsafeRunSync()
    Files.delete(Paths.get(fileName))
  }
}

@State(Scope.Benchmark)
class FormatterBenchmarks extends OdinBenchmarks {
  @Benchmark
  @OperationsPerInvocation(1000)
  def defaultFormatter(): Unit = for (_ <- 1 to 1000) Formatter.default.format(loggerMessage)

  @Benchmark
  @OperationsPerInvocation(1000)
  def jsonFormatter(): Unit = for (_ <- 1 to 1000) JsonFormatter.json.format(loggerMessage)
}
// $COVERAGE-ON$
