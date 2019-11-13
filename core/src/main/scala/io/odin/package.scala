package io

import io.odin.loggers.{
  AsyncLoggerBuilder,
  ConsoleLoggerBuilder,
  ConstContextLoggerBuilder,
  ContextualLoggerBuilder,
  RouterLoggerBuilder
}

package object odin
    extends AsyncLoggerBuilder
    with ConsoleLoggerBuilder
    with ConstContextLoggerBuilder
    with ContextualLoggerBuilder
    with RouterLoggerBuilder
