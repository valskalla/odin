package io.odin.meta

import java.util.UUID

trait ToContextData[A] {
  def toContextData(a: A): ContextData
}

object ToContextData {

  def apply[A](implicit tcd: ToContextData[A]): ToContextData[A] = tcd

  /**
    * Construct [[ToContextData]] using default `.toString` method
    */
  def fromToString[M]: ToContextData[M] = (m: M) => ContextStringValue(m.toString)

  implicit val toContextDataString: ToContextData[String] =
    (m: String) => ContextStringValue(m)

  implicit val toContextDataByte: ToContextData[Byte] =
    (x: Byte) => ContextLongValue(x.toLong)

  implicit val toContextDataShort: ToContextData[Short] =
    (x: Short) => ContextLongValue(x.toLong)

  implicit val toContextDataInt: ToContextData[Int] =
    (x: Int) => ContextLongValue(x.toLong)

  implicit val toContextDataLong: ToContextData[Long] =
    (x: Long) => ContextLongValue(x)

  implicit val toContextDataDouble: ToContextData[Double] =
    (x: Double) => ContextDoubleValue(x)

  implicit val toContextDataFloat: ToContextData[Float] =
    (x: Float) => ContextDoubleValue(x.toDouble)

  implicit val toContextDataBoolean: ToContextData[Boolean] =
    (x: Boolean) => ContextBooleanValue(x)

  implicit val toContextDataUuid: ToContextData[UUID] = fromToString

  implicit def toContextDataList[A](implicit tcd: ToContextData[A]): ToContextData[List[A]] =
    (list: List[A]) => ContextList(list.map(x => tcd.toContextData(x)))

  // TODO Seq, Vector, NEL

  implicit def toContextDataMap[A](implicit tcd: ToContextData[A]): ToContextData[Map[String, A]] =
    (map: Map[String, A]) => ContextMap(map.view.mapValues(x => tcd.toContextData(x)).toMap)

}

