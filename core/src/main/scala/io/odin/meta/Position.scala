package io.odin.meta

/**
  * Position of log invocation
  */
case class Position(
    fileName: String,
    className: String,
    methodName: Option[String],
    packageName: Option[String],
    line: Option[Int],
    column: Option[Int]
)
