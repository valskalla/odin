package io.odin

import java.time.LocalDateTime

import org.scalacheck.Arbitrary
import org.scalacheck.Gen

/**
  * Utility for managing tests
  * depending on files
  */
trait FileHelper {

  val localDateTimeGen: Gen[LocalDateTime] = for {
    year <- Gen.choose(0, LocalDateTime.now().getYear)
    month <- Gen.choose(1, 12)
    day <- Gen.choose(1, 28)
    hour <- Gen.choose(0, 23)
    minute <- Gen.choose(0, 59)
    second <- Gen.choose(0, 59)
  } yield LocalDateTime.of(year, month, day, hour, minute, second)

  implicit val localDateTimeArbitrary: Arbitrary[LocalDateTime] = Arbitrary(localDateTimeGen)

}
