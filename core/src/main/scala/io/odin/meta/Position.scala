package io.odin.meta

/**
  * Position of log invocation
  */
case class Position(
    fileName: String,
    enclosureName: String,
    packageName: String,
    line: Int
)

object Position {

  implicit def derivePosition(
      implicit fileName: sourcecode.File,
      enclosureName: sourcecode.FullName,
      packageName: sourcecode.Pkg,
      line: sourcecode.Line
  ): Position =
    Position(
      fileName = fileName.value,
      enclosureName = enclosureName.value,
      packageName = packageName.value,
      line = line.value
    )

}
