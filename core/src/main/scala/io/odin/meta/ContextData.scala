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

  /*
   * Implicit conversion from ContextData to Map[String, String]
   * so we can avoid an invasive change to the Logger and LoggerMessage APIs.
   *
   * Nested keys are flattenned into a single dot-separated key.
   */
  implicit def contextData2stringStringMap(data: ContextData): Map[String, String] = {
    def flattenKeys(parentKey: String, childMap: Map[String, String]): Map[String, String] =
      childMap.map {
        case ("", childValue) =>
          parentKey -> childValue
        case (childKey, childValue) =>
          s"$parentKey.$childKey" -> childValue
      }

    data match {
      case ContextMap(map) =>
        map.flatMap {
          case (key, value) =>
            flattenKeys(key, contextData2stringStringMap(value))
        }
      case ContextList(elems) =>
        elems.zipWithIndex.flatMap {
          case (elem, i) => flattenKeys(i.toString, contextData2stringStringMap(elem))
        }.toMap
      case ContextStringValue(value) => Map("" -> value)
      case ContextLongValue(value) => Map("" -> value.toString)
      case ContextDoubleValue(value) => Map("" -> value.toString)
      case ContextBooleanValue(value) => Map("" -> value.toString)
    }
  }

}
