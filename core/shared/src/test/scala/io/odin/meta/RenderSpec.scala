package io.odin.meta

import java.util.UUID

import cats.Show
import cats.data.NonEmptyList
import cats.laws.discipline.arbitrary._
import cats.syntax.all._
import io.odin.OdinSpec
import org.scalacheck.{Arbitrary, Gen}

import scala.reflect.ClassTag

class RenderSpec extends OdinSpec {
  it should "derive Render instance from cats.Show" in {
    val renderer = Render[Foo]
    forAll { foo: Foo =>
      renderer.render(foo) shouldBe foo.show
    }
  }

  it should "use .toString" in {
    val renderer = Render.fromToString[Foo]
    forAll { foo: Foo =>
      renderer.render(foo) shouldBe foo.toString
    }
  }

  it should "interpolate a string using Render for every argument" in {
    import io.odin.syntax._

    implicit val intRender: Render[Int] = Render.fromToString

    forAll { (foo: Foo, string: String, int: Int) =>
      render"The interpolated $foo + $int = $string" shouldBe s"The interpolated ${foo.x} + $int = $string"
    }
  }

  behave like renderBehavior[Byte](_.toString)

  behave like renderBehavior[Short](_.toString)

  behave like renderBehavior[Int](_.toString)

  behave like renderBehavior[Long](_.toString)

  behave like renderBehavior[Double](_.toString)

  behave like renderBehavior[Float](_.toString)

  behave like renderBehavior[Boolean](_.toString)

  behave like renderBehavior[UUID](_.toString)

  behave like renderBehavior[Option[Int]](m => m.fold("None")(v => s"Some($v)"))

  behave like renderBehavior[Seq[Int]](m => m.mkString("Seq(", ", ", ")"))

  behave like renderBehavior[List[Int]](m => m.mkString("List(", ", ", ")"))

  behave like renderBehavior[Vector[Int]](m => m.mkString("Vector(", ", ", ")"))

  behave like renderBehavior[NonEmptyList[Int]](m => m.toList.mkString("NonEmptyList(", ", ", ")"))

  behave like renderBehavior[Iterable[Int]](m => m.mkString("IterableLike(", ", ", ")"))

  def renderBehavior[A: Render: ClassTag: Arbitrary](expected: A => String): Unit =
    it should s"render ${implicitly[ClassTag[A]].runtimeClass.getSimpleName}" in {
      forAll { a: A =>
        Render[A].render(a) shouldBe expected(a)
      }
    }

}

case class Foo(x: String)

object Foo {
  implicit val fooShow: Show[Foo] = foo => foo.x

  implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(Gen.alphaNumStr.map(Foo(_)))
}
