package io.odin.extras.derivation

import io.odin.OdinSpec
import io.odin.extras.derivation.RenderDerivationSpec._
import io.odin.extras.derivation.render._
import io.odin.meta.Render
import org.scalacheck.{Arbitrary, Gen}

class RenderDerivationSpec extends OdinSpec {

  it should "hide fields with @hidden annotation" in {
    val instance = WithHidden("my-field", 42)
    val expected = "WithHidden(field = my-field)"

    Render[WithHidden[String, Int]].render(instance) shouldBe expected
  }

  it should "mask fields with @secret annotation" in {
    val instance = WithSecret("my-field", "api-key")
    val expected = "WithSecret(field = my-field, secret = <secret>)"

    Render[WithSecret[String, String]].render(instance) shouldBe expected
  }

  it should "hash fields with @hash annotation" in {
    val instance = WithHashed("my-field", 123.4)

    // The expected hash was generated using:
    // echo -n '123.4' | shasum -a 256
    val expected =
      "WithHashed(field = my-field, hashed (sha256 hash) = 5f466d7afa48b619c7045d54b15d8d48f47e401335078e9267f5e1d942e09ca5)"

    Render[WithHashed[String, Double]].render(instance) shouldBe expected
  }

  it should "show limited number of elements with @length annotation" in {
    val instance = WithLengthLimit("my-field", List(1, 2, 3, 4, 5))
    val expected = "WithLengthLimit(field = my-field, limited = List(1, 2, 3)(2 more))"

    Render[WithLengthLimit[String, List[Int]]].render(instance) shouldBe expected
  }

  it should "ignore @length annotation when annotated type is not a subtype of Iterable" in {
    val instance = WithLengthLimit("my-field", "api-key")
    val expected = "WithLengthLimit(field = my-field, limited = api-key)"

    Render[WithLengthLimit[String, String]].render(instance) shouldBe expected
  }

  it should "derive a type class respecting annotations" in forAll { (bar: Bar) =>
    def renderFoo(foo: Foo): String =
      s"Foo(GenericClass(field = ${foo.field.field.value}, secret = <secret>, lengthLimited = ${foo.field.lengthLimited}))"

    val diff = bar.genericClass.lengthLimited.length - LengthLimit
    val suffix = if (diff > 0) s"($diff more)" else ""

    val lengthLimited =
      Render.renderList[String].render(bar.genericClass.lengthLimited.map(renderFoo).take(LengthLimit))

    val generic =
      s"GenericClass(field = ${bar.genericClass.field}, secret = <secret>, lengthLimited = $lengthLimited$suffix)"

    val expected = s"Bar(genericClass = $generic)"

    Render[Bar].render(bar) shouldBe expected
  }

  val valueClassGen: Gen[ValueClass] =
    for {
      int <- Gen.posNum[Int]
    } yield ValueClass(int)

  val fooGen: Gen[Foo] =
    for {
      field <- valueClassGen
      hidden <- Gen.negNum[Long]
      secret <- nonEmptyStringGen
      lengthLimited <- nonEmptyStringGen
    } yield Foo(GenericClass(field, hidden, secret, lengthLimited))

  val barGen: Gen[Bar] =
    for {
      field <- nonEmptyStringGen
      hidden <- valueClassGen
      secret <- Gen.listOf(nonEmptyStringGen)
      lengthLimited <- Gen.listOf(fooGen)
    } yield Bar(GenericClass(field, hidden, secret, lengthLimited))

  implicit val barArbitrary: Arbitrary[Bar] = Arbitrary(barGen)

}

object RenderDerivationSpec {

  val LengthLimit: Int = 3

  final case class WithHidden[A, B](field: A, @hidden hidden: B) derives Render

  final case class WithSecret[A, B](field: A, @secret secret: B) derives Render

  final case class WithHashed[A, B](field: A, @hash hashed: B) derives Render

  final case class WithLengthLimit[A, B](field: A, @length(LengthLimit) limited: B) derives Render

  final case class GenericClass[A, B, C, D](
      field: A,
      @hidden hidden: B,
      @secret secret: C,
      @length(LengthLimit) lengthLimited: D
  ) derives Render

  final case class ValueClass(value: Int) extends AnyVal

  // Remove this custom instance with a "derives Render" once magnolia supports value classes on Scala 3
  object ValueClass {
    implicit val render: Render[ValueClass] = vc => Render[Int].render(vc.value)
  }

  @rendered(includeMemberName = false)
  final case class Foo(field: GenericClass[ValueClass, Long, String, String]) derives Render

  final case class Bar(genericClass: GenericClass[String, ValueClass, List[String], List[Foo]]) derives Render

}
