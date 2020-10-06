package de.upb.cs.uc4.shared.client.kafka

import play.api.libs.json.{ Format, Json }

/** Wraps an encrypted object and its original class type
  *
  * @param ClassType of the encrypted object, to be consistent use the canonical name of the class
  * @param data encrypted payload object
  */
case class EncryptionContainer(ClassType: String, data: Array[Byte])

object EncryptionContainer {
  implicit val format: Format[EncryptionContainer] = Json.format
}