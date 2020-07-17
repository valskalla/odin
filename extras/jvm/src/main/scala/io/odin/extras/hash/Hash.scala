package io.odin.extras.hash

import java.math.BigInteger
import java.security.MessageDigest

private[extras] object Hash extends Hasher {

  def hash(value: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))
    String.format("%064x", new BigInteger(1, digest.digest()))
  }

}
