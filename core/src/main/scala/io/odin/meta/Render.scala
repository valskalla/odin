package io.odin.meta

import cats.Show

/**
  * Type class that defines how message of type `M` got rendered into String
  */
trait Render[M] {

  def render(m: M): String

}

object Render {

  /**
    * Construct [[Render]] using default `.toString` method
    */
  def fromToString[M]: Render[M] = (m: M) => m.toString

  /**
    * Automatically derive [[Render]] instance given `cats.Show`
    */
  implicit def fromShow[M](implicit S: Show[M]): Render[M] = (m: M) => S.show(m)

  implicit val renderString: Render[String] = (m: String) => m

}
