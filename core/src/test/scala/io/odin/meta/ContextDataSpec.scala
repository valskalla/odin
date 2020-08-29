package io.odin.meta

import io.odin.OdinSpec

class ContextDataSpec extends OdinSpec {

  it should "convert to a Map[String, String]" in {
    val data =
      ContextMap(Map(
        "a" -> ContextStringValue("hello"),
        "b" -> ContextLongValue(123L),
        "c" -> ContextDoubleValue(4.56),
        "d" -> ContextBooleanValue(true),
        "e" -> ContextList(List(
          ContextStringValue("world"),
          ContextLongValue(789L),
          ContextDoubleValue(6.54),
          ContextBooleanValue(false),
          ContextMap(Map(
            "a" -> ContextStringValue("wow"),
            "b" -> ContextLongValue(1234L),
            "c" -> ContextDoubleValue(4.32)))))))

    val expected = Map(
      "a" -> "hello",
      "b" -> "123",
      "c" -> "4.56",
      "d" -> "true",
      "e.0" -> "world",
      "e.1" -> "789",
      "e.2" -> "6.54",
      "e.3" -> "false",
      "e.4.a" -> "wow",
      "e.4.b" -> "1234",
      "e.4.c" -> "4.32"
    )

    ContextData.contextData2stringStringMap(data) shouldBe expected
  }

}
