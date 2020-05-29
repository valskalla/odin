package io.odin.extras.derivation

import scala.annotation.Annotation

final case class rendered(includeMemberName: Boolean) extends Annotation

final case class secret() extends Annotation

final case class hash() extends Annotation

final case class hidden() extends Annotation

final case class length(max: Int) extends Annotation
