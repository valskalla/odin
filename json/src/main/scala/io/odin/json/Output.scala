package io.odin.json

import cats.syntax.show._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.odin.Level

final private[json] case class Output(
    level: Level,
    message: String,
    context: Map[String, String],
    exception: Option[String],
    position: String,
    threadName: String,
    timestamp: String
)

object Output {
  implicit private[json] val levelCodec: JsonValueCodec[Level] = new JsonValueCodec[Level] {

    // we never decode these
    override def decodeValue(in: JsonReader, default: Level): Level = ???

    override def encodeValue(x: Level, out: JsonWriter): Unit = out.writeVal(x.show)

    override def nullValue: Level = null

  }

  implicit private[json] val codec: JsonValueCodec[Output] = JsonCodecMaker.make(
    CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.enforce_snake_case)
  )
}
