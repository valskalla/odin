package io.odin.meta

import cats.Show
import io.odin.OdinSpec
import org.scalacheck.{Arbitrary, Gen}
import cats.syntax.all._

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

}

case class Foo(x: String)

object Foo {
  implicit val fooShow: Show[Foo] = foo => foo.x

  implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(Gen.alphaNumStr.map(Foo(_)))
}
