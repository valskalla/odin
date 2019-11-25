package io.odin.meta

import cats.Show
import io.odin.OdinSpec
import org.scalacheck.{Arbitrary, Gen}
import cats.syntax.all._

class RenderSpec extends OdinSpec {
  it should "derive Render instance from cats.Show" in {
    val renderer = implicitly[Render[Foo]]
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
}

case class Foo(x: String)

object Foo {
  implicit val fooShow: Show[Foo] = foo => foo.x

  implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(Gen.alphaNumStr.map(Foo(_)))
}
