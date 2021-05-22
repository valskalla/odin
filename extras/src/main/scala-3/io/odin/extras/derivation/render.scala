package io.odin.extras.derivation

import io.odin.meta.Render
import magnolia.*
import java.security.MessageDigest
import java.math.BigInteger

object render extends Derivation[Render] {

  type Typeclass[A] = Render[A]

  def join[A](ctx: CaseClass[Typeclass, A]): Typeclass[A] = value => {
    if (ctx.isValueClass) {
      ctx.params.headOption.fold("")(param => param.typeclass.render(param.deref(value)))
    } else {
      val includeMemberNames = RenderUtils.includeMemberNames(ctx)

      val params = ctx.params
        .filterNot(RenderUtils.isHidden)
        .collect {
          case param if RenderUtils.isSecret(param) =>
            RenderUtils.renderParam(param.label, RenderUtils.SecretPlaceholder, includeMemberNames)

          case param if RenderUtils.shouldBeHashed(param) =>
            val label = s"${param.label} (sha256 hash)"
            val plaintext = param.typeclass.render(param.deref(value))
            RenderUtils.renderParam(label, RenderUtils.sha256(plaintext), includeMemberNames)

          case RenderUtils.hasLengthLimit(param, limit) =>
            RenderUtils.renderWithLimit(param, value, limit, includeMemberNames)

          case p =>
            RenderUtils.renderParam(p.label, p.typeclass.render(p.deref(value)), includeMemberNames)
        }

      s"${ctx.typeInfo.short}(${params.mkString(", ")})"
    }
  }

  def split[A](ctx: SealedTrait[Typeclass, A]): Typeclass[A] = value => {
    ctx.choose(value)(sub => sub.typeclass.render(sub.cast(value)))
  }

  extension(r: Render.type) {
    inline def derived[A: scala.deriving.Mirror.Of]: Render[A] = render.derived[A]
  }

}

private object RenderUtils {

  import magnolia.CaseClass.Param

  val SecretPlaceholder = "<secret>"

  @inline def includeMemberNames[A](ctx: CaseClass[Render, A]): Boolean =
    ctx.annotations
      .collectFirst { case rendered(v) =>
        v
      }
      .getOrElse(true)

  @inline def isSecret[A](param: Param[Render, A]): Boolean =
    param.annotations.contains(secret())

  @inline def shouldBeHashed[A](param: Param[Render, A]): Boolean =
    param.annotations.contains(hash())

  @inline def isHidden[A](param: Param[Render, A]): Boolean =
    param.annotations.contains(hidden())

  @inline def renderParam(label: String, value: String, includeMemberName: Boolean): String =
    if (includeMemberName) s"$label = $value" else value

  def renderWithLimit[A](param: Param[Render, A], value: A, limit: Int, includeMemberNames: Boolean): String =
    param.deref(value) match {
      case c: Iterable[_] =>
        val diff = c.iterator.length - limit
        val suffix = if (diff > 0) s"($diff more)" else ""
        val value = param.typeclass.render(c.take(limit).asInstanceOf[param.PType]) + suffix

        renderParam(param.label, value, includeMemberNames)

      case other =>
        renderParam(param.label, param.typeclass.render(other), includeMemberNames)
    }

  def sha256(value: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))
    String.format("%064x", new BigInteger(1, digest.digest()))
  }

  object hasLengthLimit {
    def unapply[A](arg: Param[Render, A]): Option[(Param[Render, A], Int)] =
      arg.annotations.collectFirst { case length(limit) =>
        (arg, limit)
      }
  }

}
