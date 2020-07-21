package io.odin.extras.hash

import com.dedipresta.crypto.hash.sha256.Sha256

private[extras] object Hash extends Hasher {

  def hash(value: String): String = Sha256.hashString(value)

}
