package io.odin

import java.time.LocalDateTime

import io.odin.internal.StringContextLength

import scala.annotation.tailrec

package object config extends Routing with FileNamePatternSyntax {

  implicit class FileNamePatternInterpolator(private val sc: StringContext) extends AnyVal {

    def file(ps: FileNamePattern*): LocalDateTime => String = {
      StringContextLength.checkLength(sc, ps)
      dt => {
        @tailrec
        def rec(args: List[FileNamePattern], parts: List[String], acc: StringBuilder): String = {
          args match {
            case Nil          => acc.append(parts.head).toString()
            case head :: tail => rec(tail, parts.tail, acc.append(parts.head).append(head.extract(dt)))
          }
        }
        rec(ps.toList, sc.parts.toList, new StringBuilder())
      }
    }

  }

  implicit def str2fileNamePattern(str: String): FileNamePattern = {
    Value(str)
  }
}
