package io.odin.examples

import cats.effect.{IO, IOApp}
import io.odin._
import io.odin.formatter.Formatter
import io.odin.formatter.options.ThrowableFormat

/**
  * Filters stack trace of the thrown exception.
  * Prints:
  * {{{
  *   ERROR io.odin.examples.FilteringStackTrace.run:39 - This is an exception
  * Caused by: java.lang.RuntimeException: here
  * io.odin.examples.FilteringStackTrace\$.run(FilteringStackTrace.scala:39)
  * io.odin.examples.FilteringStackTrace\$.main(FilteringStackTrace.scala:30)
  * io.odin.examples.FilteringStackTrace.main(FilteringStackTrace.scala)
  * "
  *}}}
  * Instead of:
  *{{{
  *   ERROR io.odin.examples.FilteringStackTrace.run:39 - This is an exception
  * Caused by: java.lang.RuntimeException: here
  * io.odin.examples.FilteringStackTrace\$.run(FilteringStackTrace.scala:39)
  * cats.effect.IOApp.\$anonfun\$main\$3(IOApp.scala:68)
  * cats.effect.internals.IOAppPlatform\$.mainFiber(IOAppPlatform.scala:40)
  * cats.effect.internals.IOAppPlatform\$.main(IOAppPlatform.scala:25)
  * cats.effect.IOApp.main(IOApp.scala:68)
  * cats.effect.IOApp.main\$(IOApp.scala:67)
  * io.odin.examples.FilteringStackTrace\$.main(FilteringStackTrace.scala:30)
  * io.odin.examples.FilteringStackTrace.main(FilteringStackTrace.scala)
  *}}}
  */
object FilteringStackTrace extends IOApp.Simple {
  val throwableFormat: ThrowableFormat = ThrowableFormat(
    ThrowableFormat.Depth.Fixed(3),
    ThrowableFormat.Indent.NoIndent,
    ThrowableFormat.Filter.Excluding("cats.effect.IOApp", "cats.effect.internals.IOAppPlatform")
  )
  val logger: Logger[IO] = consoleLogger(formatter = Formatter.create(throwableFormat, colorful = true))

  def run: IO[Unit] =
    logger.error("This is an exception", new RuntimeException("here"))
}
