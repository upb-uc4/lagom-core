package de.upb.cs.uc4.shared.server

import java.util.Base64

import javax.crypto.{ SecretKey, SecretKeyFactory }
import javax.crypto.spec.{ PBEKeySpec, SecretKeySpec }

object SecretsUtility {

  /** Used to derive a symmetric 256 bit key from a master secret and a random salt defined in the configurations
    */
  def deriveKey(key: String, salt: String): SecretKey = {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
    val spec = new PBEKeySpec(
      key.toCharArray,
      salt.getBytes,
      65536,
      256
    )
    new SecretKeySpec(factory.generateSecret(spec).getEncoded, "AES")
  }

  implicit class EncodeKey(val secretKey: SecretKey) {
    def encodeKey: String = Base64.getEncoder.encodeToString(secretKey.getEncoded)
  }
}
