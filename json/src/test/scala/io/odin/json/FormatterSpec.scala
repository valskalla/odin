package io.odin.json

import java.time.Instant

import cats.Eval
import io.odin.Level
import io.odin.LoggerMessage
import io.odin.OdinSpec
import io.odin.meta.Position

class FormatterSpec extends OdinSpec {
  "json.format" should "generate correct json" in {
    val jsonString = Formatter.json.format(
      LoggerMessage(
        Level.Info,
        Eval.later("just a test"),
        Map("a" -> "field"),
        Some(new Exception("test exception")),
        Position.derivePosition,
        "test-thread-1",
        Instant.EPOCH.toEpochMilli()
      )
    )

    // can't be bothered to pull in a proper json library to decode this and the timestamp
    // changes depending on environment and thus a bit weird way of checking the json
    jsonString should include(""""level":"INFO"""")
    jsonString should include(""""message":"just a test"""")
    jsonString should include(""""context":{"a":"field"}""")
    jsonString should include(""""exception":"Caused by: java.lang.Exception: test exception""")
    jsonString should include(""""position":"io.odin.json.FormatterSpec#jsonString:19"""")
    jsonString should include(""""thread_name":"test-thread-1"""")
    jsonString should include(""""timestamp":"1970-01-01""")
  }

  it should "serialize any LoggerMessage" in {
    forAll(loggerMessageGen) { m =>
      noException should be thrownBy Formatter.json.format(m)
    }
  }
}
