package io.odin.meta

import io.circe.{Encoder, Json, JsonObject}
import io.circe.syntax._

sealed trait ContextData

final case class ContextMap(map: Map[String, ContextData]) extends ContextData
final case class ContextList(list: List[ContextData]) extends ContextData
final case class ContextStringValue(value: String) extends ContextData
final case class ContextLongValue(value: Long) extends ContextData
final case class ContextDoubleValue(value: Double) extends ContextData
final case class ContextBooleanValue(value: Boolean) extends ContextData

object ContextData {

  implicit val jsonEncoder: Encoder[ContextData] = Encoder.instance[ContextData](_ match {
    case ContextMap(map) => Json.fromJsonObject(JsonObject.fromMap(map.view.mapValues(_.asJson).toMap))
    case ContextList(list) => Json.fromValues(list.map(_.asJson))
    case ContextStringValue(value) => Json.fromString(value)
    case ContextLongValue(value) => Json.fromLong(value)
    case ContextDoubleValue(value) => Json.fromDoubleOrString(value)
    case ContextBooleanValue(value) => Json.fromBoolean(value)
  })

}
