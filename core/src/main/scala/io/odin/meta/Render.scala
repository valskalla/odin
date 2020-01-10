package io.odin.meta

import cats.Show

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

object Render extends LowPriorityRender {

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
}

trait LowPriorityRender {

  /**
    * Automatically derive [[Render]] instance given `cats.Show`
    */
  implicit def fromShow[M](implicit S: Show[M]): Render[M] = (m: M) => S.show(m)
}
