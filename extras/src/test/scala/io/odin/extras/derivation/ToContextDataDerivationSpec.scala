package io.odin.extras.derivation

import io.odin.meta.{Foo => _, _}
import io.odin.OdinSpec
import io.odin.extras.derivation.ToContextDataDerivationSpec._
import io.odin.extras.derivation.context._
import org.scalacheck.{Arbitrary, Gen}

class ToContextDataDerivationSpec extends OdinSpec {

  it should "hide fields with @hidden annotation" in {
    val instance = WithHidden("my-field", 42)
    val expected = ContextMap(Map("field" -> ContextStringValue("my-field")))

    ToContextData[WithHidden[String, Int]].toContextData(instance) shouldBe expected
  }

  it should "mask fields with @secret annotation" in {
    val instance = WithSecret("my-field", "api-key")
    val expected = ContextMap(Map(
      "field" -> ContextStringValue("my-field"),
      "secret" -> ContextStringValue("<secret>"),
    ))

    ToContextData[WithSecret[String, String]].toContextData(instance) shouldBe expected
  }

  //it should "show limited number of elements with @length annotation" in {
  //  val instance = WithLengthLimit("my-field", List(1, 2, 3, 4, 5))
  //  val expected = "WithLengthLimit(field = my-field, limited = List(1, 2, 3)(2 more))"

  //  ToContextData[WithLengthLimit[String, List[Int]]].render(instance) shouldBe expected
  //}

  //it should "ignore @length annotation when annotated type is not a subtype of Iterable" in {
  //  val instance = WithLengthLimit("my-field", "api-key")
  //  val expected = "WithLengthLimit(field = my-field, limited = api-key)"

  //  ToContextData[WithLengthLimit[String, String]].render(instance) shouldBe expected
  //}

  it should "derive a type class respecting annotations" in forAll { bar: Bar =>
    def fooToContextData(foo: Foo): ContextData =
      ContextMap(Map(
        "field" -> ContextMap(Map(
          "field" -> ContextLongValue(foo.field.field.value.toLong),
          "secret" -> ContextStringValue("<secret>"),
          "anotherField" -> ContextDoubleValue(foo.field.anotherField)
        ))
      ))

    val generic =
      ContextMap(Map(
        "field" -> ContextStringValue(bar.genericClass.field),
        "secret" -> ContextStringValue("<secret>"),
        "anotherField" -> ContextList(bar.genericClass.anotherField.map(fooToContextData))
      ))

    val expected = ContextMap(Map("genericClass" -> generic))

    ToContextData[Bar].toContextData(bar) shouldBe expected
  }

  it should "not recursively generate instances by `instance` method" in {
    assertDoesNotCompile {
      """
        case class A(field: String)
        case class B(field: A)
        io.odin.extras.derivation.context.instance[B]
      """
    }
  }

  it should "recursively generate instances by `derive` method" in {
    assertCompiles {
      """
        case class A(field: String)
        case class B(field: A)
        io.odin.extras.derivation.context.derive[B]
      """
    }
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
      anotherField <- Gen.posNum[Double]
    } yield Foo(GenericClass(field, hidden, secret, anotherField))

  val barGen: Gen[Bar] =
    for {
      field <- nonEmptyStringGen
      hidden <- valueClassGen
      secret <- Gen.listOf(nonEmptyStringGen)
      anotherField <- Gen.listOf(fooGen)
    } yield Bar(GenericClass(field, hidden, secret, anotherField))

  implicit val barArbitrary: Arbitrary[Bar] = Arbitrary(barGen)

}

object ToContextDataDerivationSpec {

  val LengthLimit: Int = 3

  final case class WithHidden[A, B](field: A, @hidden hidden: B)

  final case class WithSecret[A, B](field: A, @secret secret: B)

  final case class WithLengthLimit[A, B](field: A, @length(LengthLimit) limited: B)

  final case class GenericClass[A, B, C, D](
      field: A,
      @hidden hidden: B,
      @secret secret: C,
      anotherField: D
  )

  final case class ValueClass(value: Int) extends AnyVal

  final case class Foo(field: GenericClass[ValueClass, Long, String, Double])

  final case class Bar(genericClass: GenericClass[String, ValueClass, List[String], List[Foo]])

}
