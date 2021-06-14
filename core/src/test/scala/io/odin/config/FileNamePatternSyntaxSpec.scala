package io.odin.config

import java.time.LocalDateTime

import io.odin.OdinSpec

class FileNamePatternSyntaxSpec extends OdinSpec {

  def checkPadding(value: Int): String = {
    if (value < 10) s"0$value" else value.toString
  }

  "year" should "extract current year" in {
    forAll { (dt: LocalDateTime) =>
      year.extract(dt) shouldBe checkPadding(dt.getYear)
    }
  }

  "month" should "extract current month" in {
    forAll { (dt: LocalDateTime) =>
      month.extract(dt) shouldBe checkPadding(dt.getMonthValue)
    }
  }

  "day" should "extract current day" in {
    forAll { (dt: LocalDateTime) =>
      day.extract(dt) shouldBe checkPadding(dt.getDayOfMonth)
    }
  }

  "hour" should "extract current hour" in {
    forAll { (dt: LocalDateTime) =>
      hour.extract(dt) shouldBe checkPadding(dt.getHour)
    }
  }

  "minute" should "extract current minute" in {
    forAll { (dt: LocalDateTime) =>
      minute.extract(dt) shouldBe checkPadding(dt.getMinute)
    }
  }

  "second" should "extract current second" in {
    forAll { (dt: LocalDateTime) =>
      second.extract(dt) shouldBe checkPadding(dt.getSecond)
    }
  }

  "file" should "process a single argument" in {
    forAll { (dt: LocalDateTime) =>
      file"$year".apply(dt) shouldBe checkPadding(dt.getYear)
    }
  }

  "file" should "process an argument in the beginning" in {
    forAll { (dt: LocalDateTime) =>
      file"$year year".apply(dt) shouldBe s"${checkPadding(dt.getYear)} year"
    }
  }

  "file" should "process an argument in the end" in {
    forAll { (dt: LocalDateTime) =>
      file"Year $year".apply(dt) shouldBe s"Year ${checkPadding(dt.getYear)}"
    }
  }

  "file" should "process argument in the middle" in {
    forAll { (dt: LocalDateTime) =>
      file"It's $year year".apply(dt) shouldBe s"It's ${checkPadding(dt.getYear)} year"
    }
  }

  "file" should "process multiple arguments" in {
    forAll { (dt: LocalDateTime) =>
      file"$year-$month-$day"
        .apply(dt) shouldBe s"${checkPadding(dt.getYear)}-${checkPadding(dt.getMonthValue)}-${checkPadding(dt.getDayOfMonth)}"
    }
  }

  "file" should "process a string variable" in {
    forAll { (dt: LocalDateTime, str: String) =>
      file"$str".apply(dt) shouldBe str
    }
  }

}
