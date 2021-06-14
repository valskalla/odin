package io.odin.util

import java.io.File
import java.nio.file.{Files, Path}
import java.util.stream.Collectors
import scala.jdk.CollectionConverters._

object ListDirectory {

  def apply(path: Path): List[File] = {
    Files.list(path).collect(Collectors.toList[Path]).asScala.toList.map(_.toFile)
  }

}
