package de.upb.cs.uc4.shared.client

import java.nio.charset.Charset

/** The Hashing object.
  *
  * Provides an interface to SHA hash functions of different length. Hash functions can be queried to receive a string
  * hash of a string input.
  */
object Hashing {

  /** Calculates SHA hash of length 256 bit
    *
    * @param input byte array to hash
    * @return hashed input
    */
  def sha256(input: Array[Byte]): String =
    com.google.common.hash.Hashing.sha256().hashBytes(input).toString

  /** Calculates SHA hash of length 256 bit
    *
    * @param input string to hash
    * @return hashed input
    */
  def sha256(input: String): String =
    com.google.common.hash.Hashing.sha256().hashString(input, Charset.defaultCharset()).toString

  /** Calculates SHA hash of length 384 bit
    *
    * @param input string to hash
    * @return hashed input
    */
  def sha384(input: String): String =
    com.google.common.hash.Hashing.sha384().hashString(input, Charset.defaultCharset()).toString

  /** Calculates SHA hash of length 512 bit
    *
    * @param input string to hash
    * @return hashed input
    */
  def sha512(input: String): String =
    com.google.common.hash.Hashing.sha512().hashString(input, Charset.defaultCharset()).toString
}
