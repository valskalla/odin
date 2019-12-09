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

Setup
---

Odin is published to Maven Central and cross-built for Scala 2.12 and 2.13. Add the following lines to your build:

```scala
libraryDependencies ++= Seq(
  "com.github.valskalla" %% "odin-core",
  "com.github.valskalla" %% "odin-json" //to enable JSON formatter if needed
).map(_ % "0.3.0")
```

Effects out of the box
---

Some time could be saved by using the effect-predefined variants of Odin. There are options for ZIO and Monix users: 

```scala
//ZIO
libraryDependencies += "com.github.valskalla" %% "odin-zio" % "0.3.0"
//or Monix
libraryDependencies += "com.github.valskalla" %% "odin-monix" % "0.3.0"
```

Use corresponding import to get an access to the loggers:

```scala
import io.odin.zio._
//or
import io.odin.monix._
```

Documentation
---

- [Example](#example)
- [Logger Interface](#logger-interface)
- [Render](#render)
- [Console Logger](#console-logger)
- [Formatter](#formatter)
  - [JSON Formatter](#json-formatter)
- [Minimal level](#minimal-level)
- [File logger](#file-logger)
- [Async logger](#async-logger)
- [Class and enclosure routing](#class-and-enclosure-routing)
- [Loggers composition](#loggers-composition)
- [Constant context](#constant-context)
- [Contextual effects](#contextual-effects)
- [Contramap and filter](#contramap-and-filter)

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
the logger methods isn't enough to emit the actual log. User has to to combine log operations with the rest of code
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
import cats.effect.{ContextShift, IO}
import cats.effect.Timer

//required to derive Concurrent/ConcurrentEffect for async operations with IO later. IOApp provides it out of the box
implicit val contextShiftIO: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

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
//     Delay(cats.effect.Clock$$anon$1$$Lambda$8390/453278552@6c14f80),
//     io.odin.loggers.DefaultLogger$$Lambda$8391/800998867@6f5d5198
//   ),
//   scala.Function1$$Lambda$8398/1404304763@4e6c7386,
//   1
// )

//prints "Hello world" to the STDOUT.
//Although, don't use `unsafeRunSync` in production unless you know what you're doing
logger.info("Hello world").unsafeRunSync()
// 2019-12-09T12:09:22 [run-main-0] INFO repl.Session.App#res1:68 - Hello world
```

All messages of level `WARN` and higher are routed to the _STDERR_ while messages with level `INFO` and below go to the _STDOUT_.

`consoleLogger` has the following definition:

```scala
def consoleLogger[F[_]: Sync: Timer](
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Logger[F]
```

It's possible to configure minimal level of logs to print (_TRACE_ by default) and formatter that's used to print it.

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
// 2019-12-09T12:09:22 [run-main-0] INFO repl.Session.App#res2:77 - No context
// 2019-12-09T12:09:22 [run-main-0] INFO repl.Session.App#res2:77 - Some context - key: value
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
// {"level":"INFO","message":"This is JSON","context":{},"exception":null,"position":"repl.Session.App#res3:92","thread_name":"run-main-0","timestamp":"2019-12-09T12:09:22"}
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

## File logger

Another backend that Odin provides by default is the basic file logger:

```scala
def fileLogger[F[_]: Sync: Timer](
      fileName: String,
      formatter: Formatter = Formatter.default,
      minLevel: Level = Level.Trace
  ): Resource[F, Logger[F]]
```

So far it's capable only of writing to the single file by path `fileName`. One particular detail is worth to mention here:
return type. Odin tries to guarantee safe allocation and release of file resource. Because of that `fileLogger` returns
`Resource[F, Logger[F]]` instead of `Logger[F]`:

```scala
val file = fileLogger[IO]("log.log")

file.use { logger =>
  logger.info("Hello file")
}.unsafeRunSync() //Mind that here file resource is free, all buffers are flushed and closed
```

Usual pattern here is to compose and allocate such resources during the start of your program and wrap execution of
the logic inside of `.use` block.  

**Important notice**: this logger doesn't buffer and tries to flush to the file on each log due to the safety guarantees.
Consider to use `asyncFileLogger` version with almost the same signature (except the `Concurrent[F]` constraint)
to achieve the best performance.

## Async logger

To achieve the best performance with Odin, it's best to use `AsyncLogger` with the combination of any other logger.
It uses `ConcurrentQueue[F]` from Monix as the buffer that is asynchronously flushed each fixed time period.

Conversion of any logger into async one is straightforward:

```scala
import cats.effect.Resource
import io.odin.syntax._ //to enable additional implicit methods

val asyncLoggerResource: Resource[IO, Logger[IO]] = consoleLogger[IO]().withAsync()
```

`Resource[F, Logger[F]]` is used to properly initialize the log buffer and flush it on the release. Therefore, use of
async logger shall be done inside of `Resource.use` block:
 
```scala
//queue will be flushed on release even if flushing timer didn't hit the mark yet
asyncLoggerResource.use(logger => logger.info("Async info")).unsafeRunSync()
// 2019-12-09T12:09:23 [run-main-0] INFO repl.Session.App#res6 $anonfun:142 - Async info
```

Package `io.odin.syntax._` also pimps the `Resource[F, Logger[F]]` type with the same `.withAsync` method to use
in combination with i.e. `fileLogger`. Actually, `asyncFileLogger` implemented exactly in this manner.
The immediate gain is the acquire/release safety provided by `Resource` abstraction for combination of `FileLogger` and `AsyncLogger`,
as well as controllable in-memory buffering for logs before they're flushed down the stream.

Definition of `withAsync` is following:
```scala
def withAsync(
        timeWindow: FiniteDuration = 1.millis,
        maxBufferSize: Option[Int] = None
    )(implicit timer: Timer[F], F: Concurrent[F], contextShift: ContextShift[F]): Resource[F, Logger[F]]
```

Following parameters are configurable if default ones don't fit:
- Time period between flushed, default is 1 millisecond
- Maximum underlying buffer size, by default buffer is unbounded.

## Class and enclosure routing

Users have an option to use different loggers for different classes, packages and even function enclosures.
I.e. it could be applied to silence some particular class applying stricter minimal level requirements.  

Class based routing works with the help of `classOf` function:

```scala
import io.odin.config._

case class Foo[F[_]](logger: Logger[F]) {
  def log: F[Unit] = logger.info("foo")
}

case class Bar[F[_]](logger: Logger[F]) {
  def log: F[Unit] = logger.info("bar")
}

val routerLogger: Logger[IO] =
    classRouting[IO](
      classOf[Foo[IO]] -> consoleLogger[IO]().withMinimalLevel(Level.Warn),
      classOf[Bar[IO]] -> consoleLogger[IO]().withMinimalLevel(Level.Info)
    ).withNoopFallback
```

Method `withNoopFallback` defines that any message that doesn't match the router is nooped.
Use `withFallback(logger: Logger[F])` to route all non-matched messages to specific logger.

Enclosure based routing is more flexible but might be error-prone as well:

```scala
val enclosureLogger: Logger[IO] =
    enclosureRouting(
      "io.odin.foo" -> consoleLogger[IO]().withMinimalLevel(Level.Warn),
      "io.odin.bar" -> consoleLogger[IO]().withMinimalLevel(Level.Info),
      "io.odin" -> consoleLogger[IO]()
    )
    .withNoopFallback

def zoo: IO[Unit] = enclosureLogger.debug("Debug")
def foo: IO[Unit] = enclosureLogger.info("Never shown")
def bar: IO[Unit] = enclosureLogger.warn("Warning")
```

Routing is done based on the string matching with the greedy first match logic, hence the most specific routes
should always appear on the top, otherwise they might be ignored.

## Loggers composition

Odin defines `Monoid[Logger[F]]` instance to combine multiple loggers into a single one that broadcasts
the logs to the underlying loggers:

```scala
import cats.syntax.all._

val combinedLogger: Resource[IO, Logger[IO]] = consoleLogger[IO]().withAsync() |+| fileLogger[IO]("log.log")
```

Example above demonstrates that it would work even for `Resource` based loggers. `combinedLogger` prints messages both
to console and to _log.log_ file.

## Constant context

To append some predefined context to all the messages of the logger, use `withConstContext` syntax to construct such logger:

```scala
import io.odin.syntax._

consoleLogger[IO]()
    .withConstContext(Map("predefined" -> "context"))
    .info("Hello world").unsafeRunSync()
// 2019-12-09T12:09:23 [run-main-0] INFO repl.Session.App#res7:206 - Hello world - predefined: context
```

## Contextual effects

Some effects carry an extractable information that could be automatically injected as the context to each log message.
An example is `ReaderT[F[_], Env, A]` monad, where value of type `Env` contains some additional information to log.

Odin allows to build a logger that extracts this information from effect and put it as the context:

```scala
import io.odin.loggers._
import cats.data.ReaderT
import cats.mtl.instances.all._ //provides ApplicativeAsk instance for ReaderT //provides ApplicativeAsk instance for ReaderT

case class Env(ctx: Map[String, String])

object Env {
  //it's neccessary to describe how to extract context from env
  implicit val hasContext: HasContext[Env] = new HasContext[Env] {
    def getContext(env: Env): Map[String, String] = env.ctx
  }
}

type M[A] = ReaderT[IO, Env, A]

consoleLogger[M]()
    .withContext
    .info("Hello world")
    .run(Env(Map("env" -> "ctx")))
    .unsafeRunSync()
// 2019-12-09T12:09:23 [run-main-0] INFO repl.Session.App#res8:237 - Hello world - env: ctx
```

Odin automatically derives required type classes for each type `F[_]` that has `ApplicativeAsk[F, E]` defined, or in other words
for all the types that allow `F[A] => F[E]`.

If this constraint isn't satisfied, it's required to manually provide an instance for `WithContext` type class:  
```scala
/**
  * Resolve context stored in `F[_]` effect
  */
trait WithContext[F[_]] {
  def context: F[Map[String, String]]
}
```

## Contramap and filter

To modify or filter log messages before they're written, use corresponding combinators `contramap` and `filter`:

```scala
import io.odin.syntax._

consoleLogger[IO]()
    .contramap(msg => msg.copy(message = msg.message.map(_ + " World")))
    .info("Hello")
    .unsafeRunSync()
// 2019-12-09T12:09:23 [run-main-0] INFO repl.Session.App#res9:250 - Hello World

consoleLogger[IO]()
    .filter(msg => msg.message.value.size < 10)
    .info("Very long messages are discarded")
    .unsafeRunSync()
```

Contributing
---
Feel free to open an issue, submit a Pull Request or ask [in the Gitter channel](https://gitter.im/valskalla/odin).
We strive to provide a welcoming environment for everyone with good intentions.

Also, don't hesitate to give it a star and spread the word to your friends and colleagues.

License
---
Licensed under the **[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.