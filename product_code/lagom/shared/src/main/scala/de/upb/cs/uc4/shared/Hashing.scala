package de.upb.cs.uc4.shared

import java.nio.charset.Charset

object Hashing {

  def sha256(input: String): String =
    com.google.common.hash.Hashing.sha256().hashString(input, Charset.defaultCharset()).toString

  def sha384(input: String): String =
    com.google.common.hash.Hashing.sha384().hashString(input, Charset.defaultCharset()).toString

  def sha512(input: String): String =
    com.google.common.hash.Hashing.sha512().hashString(input, Charset.defaultCharset()).toString
}
