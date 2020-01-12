package io.odin.meta

import java.util.UUID

import cats.Show
import cats.data.NonEmptyList

/**
  * Type class that defines how message of type `M` got rendered into String
  */
@scala.annotation.implicitNotFound(
  """
A value of type ${M} you're trying to log is missing a renderer.
Make sure you have an implicit Render[${M}] in scope, i.e. inside of companion object of ${M}.
"""
)
trait Render[M] {
  def render(m: M): String
}

object Render extends MidPriorityRender {

  def apply[A](implicit instance: Render[A]): Render[A] = instance

  final case class Rendered(override val toString: String) extends AnyVal
  object Rendered {
    implicit def mat[A](a: A)(implicit r: Render[A]): Rendered = Rendered(r.render(a))
  }

  /**
    * Construct [[Render]] using default `.toString` method
    */
  def fromToString[M]: Render[M] = (m: M) => m.toString

  implicit val renderString: Render[String] = (m: String) => m

  implicit val renderByte: Render[Byte] = fromToString

  implicit val renderShort: Render[Short] = fromToString

  implicit val renderInt: Render[Int] = fromToString

  implicit val renderLong: Render[Long] = fromToString

  implicit val renderDouble: Render[Double] = fromToString

  implicit val renderFloat: Render[Float] = fromToString

  implicit val renderBoolean: Render[Boolean] = fromToString

  implicit val renderUuid: Render[UUID] = fromToString

  implicit def renderOption[A](implicit r: Render[A]): Render[Option[A]] =
    (m: Option[A]) => m.fold("None")(v => s"Some(${r.render(v)})")

  implicit def renderSeq[A: Render]: Render[Seq[A]] =
    fromIteratorRender("Seq", _.iterator)

  implicit def renderList[A: Render]: Render[List[A]] =
    fromIteratorRender("List", _.iterator)

  implicit def renderVector[A: Render]: Render[Vector[A]] =
    fromIteratorRender("Vector", _.iterator)

  implicit def renderNonEmptyList[A: Render]: Render[NonEmptyList[A]] =
    fromIteratorRender("NonEmptyList", _.toList.iterator)

}

trait MidPriorityRender extends LowPriorityRender {

  implicit def renderIterable[A: Render, C[_]](implicit ev: C[A] => Iterable[A]): Render[C[A]] =
    fromIteratorRender[A, C]("IterableLike", m => ev(m).iterator)

  protected def fromIteratorRender[A: Render, C[_]](name: String, toIterator: C[A] => Iterator[A]): Render[C[A]] =
    (m: C[A]) => toIterator(m).map(Render[A].render).mkString(s"$name(", ", ", ")")

}

trait LowPriorityRender {

  /**
    * Automatically derive [[Render]] instance given `cats.Show`
    */
  implicit def fromShow[M](implicit S: Show[M]): Render[M] = (m: M) => S.show(m)
}
