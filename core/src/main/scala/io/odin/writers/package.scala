package io.odin

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

package object writers {

  /**
    * Unbounded EC is useful for I/O operations
    */
  def unboundedExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

}
