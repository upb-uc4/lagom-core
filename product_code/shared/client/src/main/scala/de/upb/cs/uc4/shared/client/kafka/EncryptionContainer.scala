package de.upb.cs.uc4.shared.client.kafka

import play.api.libs.json.{ Format, Json }

case class EncryptionContainer(dataType: String, data: Array[Byte])

object EncryptionContainer {
  implicit val format: Format[EncryptionContainer] = Json.format
}