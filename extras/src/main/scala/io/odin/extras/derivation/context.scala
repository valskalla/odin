package io.odin.extras.derivation

import io.odin.meta._
import magnolia.{CaseClass, Magnolia, Param, SealedTrait}

object context {

  type Typeclass[A] = ToContextData[A]

  def combine[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = value => {
    if (ctx.isValueClass) {
      ctx.parameters.headOption.fold[ContextData](ContextMap(Map.empty))(param =>
          param.typeclass.toContextData(param.dereference(value)))
    } else {
      val params = ctx.parameters
        .filterNot(ContextUtils.isHidden)
        .collect {
          case param if ContextUtils.isSecret(param) =>
            (param.label, ContextStringValue(ContextUtils.SecretPlaceholder))

          // TODO does it make sense to support the @length param?

          case param =>
            (param.label, param.typeclass.toContextData(param.dereference(value)))
        }
        .toMap
      ContextMap(params)
    }
  }

  def dispatch[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = value => {
    ctx.dispatch(value)(sub => sub.typeclass.toContextData(sub.cast(value)))
  }

  /**
    * Creates an instance for a concrete type. Does not generate instances for underlying types.
    *
    * Example:
    * {{{
    *   import io.odin.extras.derivation.context
    *
    *   case class A(field: String)
    *   case class B(field: A)
    *
    *   val instanceA: ToContextData[A] = context.instance[A] // compiles
    *   val instanceB: ToContextData[B] = context.instance[B] // does not compile
    * }}}
    */
  def instance[A]: Typeclass[A] = macro Magnolia.gen[A]

  /**
    * Creates an instance for a concrete type. Recursively generate instances for underlying types.
    *
    * Example:
    * {{{
    *   import io.odin.extras.derivation.context
    *
    *   case class A(field: String)
    *   case class B(field: A)
    *
    *   val instanceB: ToContextData[B] = context.derive[B]
    * }}}
    */
  implicit def derive[A]: Typeclass[A] = macro Magnolia.gen[A]

}

private object ContextUtils {

  val SecretPlaceholder = "<secret>"

  @inline def isSecret[A](param: Param[ToContextData, A]): Boolean =
    param.annotations.contains(secret())

  @inline def isHidden[A](param: Param[ToContextData, A]): Boolean =
    param.annotations.contains(hidden())

}
