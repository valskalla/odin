<p align="center">
  <img src="https://raw.githubusercontent.com/valskalla/odin/master/logo.png" width="500px" />  
  <br/>
  <i>The god of poetry (also battles, fights and other side effects)</i>
</p>

----
[![Build Status](https://github.com/valskalla/odin/workflows/Scala%20CI/badge.svg)](https://github.com/valskalla/odin/actions) [![Codecov](https://img.shields.io/codecov/c/github/valskalla/odin)](https://codecov.io/gh/valskalla/odin)

Odin library enables functional approach to logging in Scala applications with reasoning and performance being
top priorities.

- Each effect is represented with tagless `F[_]` style
- Context is a first-class citizen. No more `TheadLocal` MDCs.
- Position tracing implemented with macro instead of reflection considerably boosts the performance
- Own performant logger backends for console and log files
- Composable loggers to bring different loggers together

Standing on the shoulders of `cats-effect` type classes, Odin abstracts away from concrete effect types, allowing
users to decide what they feel comfortable with: `IO`, `ZIO`, `monix.Task`, `ReaderT` etc. The choice is yours.

Documentation
---

### Setup

Odin is published to Maven Central and cross-built for Scala 2.12 and 2.13. Add the following lines to your build:

```
//yet to publish
```

### Example

Using `IOApp`:
```scala
package io.odin.examples

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
2019-11-25 [ioapp-compute-0] INFO io.odin.examples.Simple.run:12 - Hello world
```

Check out [examples](https://github.com/valskalla/odin/tree/master/examples) directory for more