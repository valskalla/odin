<p align="center">
  <img src="https://raw.githubusercontent.com/valskalla/odin/master/logo.png" width="500px" />  
  <br/>
  <i>The god of poetry (also battles, fights and other side effects)</i>
</p>

----
[![Build Status](https://github.com/valskalla/odin/workflows/Scala%20CI/badge.svg)](https://github.com/valskalla/odin/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.valskalla/odin-core_2.13)](https://search.maven.org/search?q=com.github.valskalla)
[![Codecov](https://img.shields.io/codecov/c/github/valskalla/odin)](https://codecov.io/gh/valskalla/odin)
[![License](https://img.shields.io/github/license/valskalla/odin)](https://github.com/valskalla/odin/blob/master/LICENSE)

Odin library enables functional approach to logging in Scala applications with reasoning and performance as the
top priorities.

- Each effect is suspended within the polymorphic `F[_]`
- Context is a first-class citizen. Logger is structured by default, no more `TheadLocal` MDCs
- Programmatically configurable. Scala is the perfect language for describing configs
- Position tracing implemented with macro instead of reflection considerably boosts the performance
- Own performant logger backends for console and log files
- Composable loggers to bring different loggers together with `Monoid[Logger[F]]`

Standing on the shoulders of `cats-effect` type classes, Odin abstracts away from concrete effect types, allowing
users to decide what they feel comfortable with: `IO`, `ZIO`, `monix.Task`, `ReaderT` etc. The choice is yours.

Documentation
---

- [Setup](#setup)
- [Example](#example)
- [Logger Interface](#logger-interface)
- [Render](#render)
- [Console Logger](#console-logger)
- [Formatter](#formatter)
  - [JSON Formatter](#json-formatter)
- [Minimal level](#minimal-level)
- File logger
- Async logger
- Package and enclosure routing
- Loggers composition
- Constant context
- Contextual effects
- Contramap & filter

## Setup

Odin is published to Maven Central and cross-built for Scala 2.12 and 2.13. Add the following lines to your build:

```scala
libraryDependencies ++= Seq(
  "com.github.valskalla" %% "odin-core",
  "com.github.valskalla" %% "odin-json" //to enable JSON formatter if needed
).map(_ % "0.2.0+0-c8239d1f+20191201-1927-SNAPSHOT")
```

## Example

Using `IOApp`:
```scala
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all._
import io.odin._

object Simple extends IOApp {

  val logger: Logger[IO] = consoleLogger()

  def run(args: List[String]): IO[ExitCode] = {
    logger.info("Hello world").as(ExitCode.Success)
  }
}
```

Once application starts, it prints:
```
2019-11-25T22:00:51 [ioapp-compute-0] INFO io.odin.examples.HelloWorld.run:15 - Hello world
```

Check out [examples](https://github.com/valskalla/odin/tree/master/examples) directory for more

## Logger interface

Odin's logger interface looks like following:

```scala
trait Logger[F[_]] {
  
  def trace[M](msg: => M)(implicit render: Render[M], position: Position): F[Unit]

  def trace[M](msg: => M, t: Throwable)(implicit render: Render[M], position: Position): F[Unit]

  def trace[M](msg: => M, ctx: Map[String, String])(implicit render: Render[M], position: Position): F[Unit]

  //continues for each different log level
}
```

Each method returns `F[Unit]`, so most of the time effects are suspended in the context of `F[_]`.

**It's important to keep in memory** that effects like `IO`, `ZIO`, `Task` etc are lazily evaluated, therefore calling
the logger methods isn't enough to emit the actual log. User need to combine log operations with the rest of code
using plead of options: `for ... yield` comprehension, `flatMap/map` or `>>/*>` operators from cats library. 

Particularly interesting are the implicit arguments: `Position` and `Render[M]`.

`Position` class carries the information about invocation site: owning enclosure, package name, current line.
It's generated in compile-time using Scala macro, so cost of position tracing in runtime is close to zero.

## Render

Logger's methods are also polymorphic for messages. Users might log every type `M` that satisfies `Record` constraint:

- It has implicit `Render[M]` instance that describes how to convert value of type `M` to `String`
- Or it has implicit `cats.Show[M]` instance in scope

By default, Odin provides `Render[String]` instance out of the box as well as `Render.fromToString` method to construct
instances by calling the standard method `.toString` on type `M`:

```scala
import io.odin.meta.Render

case class Log(s: String, i: Int)

object Log {
  implicit val render: Render[Log] = Render.fromToString
}
```

## Console logger

The most common logger to use:

```scala
import io.odin._
import cats.effect.IO
import cats.effect.Timer

//required for log timestamps. IOApp provides it out of the box
implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.global)

val logger: Logger[IO] = consoleLogger[IO]()
```

Now to the call:

```scala
//doesn't print anything as the effect is suspended in IO
logger.info("Hello?")
// res0: IO[Unit] = Map(
//   Bind(
//     Delay(cats.effect.Clock$$anon$1$$Lambda$30992/0x000000080337d440@5fa948ce),
//     io.odin.loggers.DefaultLogger$$Lambda$30993/0x000000080337c040@685ae1c9
//   ),
//   scala.Function1$$Lambda$14579/0x00000008032eb840@4036f1ba,
//   1
// )

//prints "Hello world" to the STDOUT.
//Although, don't use `unsafeRunSync` in production unless you know what you're doing
logger.info("Hello world").unsafeRunSync()
// 2019-12-01T23:59:20 [run-main-15] INFO repl.Session.App#res1:65 - Hello world
```

All messages of level `WARN` and higher are routed to the _STDERR_ while messages with level `INFO` and below go to the _STDOUT_.

`consoleLogger` has the following definition:

```scala
def consoleLogger[F[_]: Sync: Timer](
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Debug
  ): Logger[F]
```

It's possible to configure minimal level of logs to print (_DEBUG_ by default) and formatter that's used to print it.

## Formatter

In Odin, formatters are responsible for rendering `LoggerMessage` data type into `String`.
`LoggerMessage` carries the information about :
- Level of the log
- Context
- Optional exception
- Timestamp
- Invocation position
- Suspended message

Formatter's definition is straightforward:

```scala
trait Formatter {
  def format(msg: LoggerMessage): String
}
```

_odin-core_ provides the `Formatter.default` that prints information in a nicely structured manner:

```scala
import cats.syntax.all._
(logger.info("No context") *> logger.info("Some context", Map("key" -> "value"))).unsafeRunSync()
// 2019-12-01T23:59:20 [run-main-15] INFO repl.Session.App#res2:74 - No context
// 2019-12-01T23:59:20 [run-main-15] INFO repl.Session.App#res2:74 - Some context - key: value
```

### JSON Formatter

Library _odin-json_ enables output of logs as newline-delimited JSON records:

```scala
import io.odin.json.Formatter

val jsonLogger = consoleLogger[IO](formatter = Formatter.json)
``` 

Now messages printed with this logger will be encoded as JSON string using circe:

```scala
jsonLogger.info("This is JSON").unsafeRunSync()
// {"level":"INFO","message":"This is JSON","context":{},"exception":null,"position":"repl.Session.App#res3:89","thread_name":"run-main-15","timestamp":"2019-12-01T23:59:20"}
```

## Minimal level

It's possible to set minimal level for log messages to i.e. disable debug logs in production mode:

```scala

//either by constructing logger with specific parameter
val minLevelInfoLogger = consoleLogger[IO](minLevel = Level.Info)

//or modifying the existing one
val minLevelWarnLogger = logger.withMinimalLevel(Level.Warn)
```

Those lines won't print anything:

```scala
(minLevelInfoLogger.debug("Invisible") *> minLevelWarnLogger.info("Invisible too")).unsafeRunSync()
```

Performance wise, it'll cost only the allocation of `F.unit` value.