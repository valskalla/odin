package io.odin.config

import java.time.LocalDateTime

trait FileNamePattern {
  def extract(dateTime: LocalDateTime): String
}

trait FileNamePatternSyntax {

  case class Value(value: String) extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = value
  }

  case object year extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getYear)
  }

  case object month extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getMonthValue)
  }

  case object day extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getDayOfMonth)
  }

  case object hour extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getHour)
  }

  case object minute extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getMinute)
  }

  case object second extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getSecond)
  }

  private[odin] def padWithZero(value: Int): String = f"$value%02d"

}
