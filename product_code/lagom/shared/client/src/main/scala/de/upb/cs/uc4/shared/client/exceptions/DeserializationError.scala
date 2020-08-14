package de.upb.cs.uc4.shared.client.exceptions

import play.api.libs.json.{Format, Json}

case class DeserializationError(`type`: String, title: String, missingFields: Seq[String]) extends CustomError

object DeserializationError {
  implicit val format: Format[DetailedError] = Json.format

  def apply(`type`: String, missingFields: Seq[String]): DeserializationError = {
    val title = CustomError.getTitle(`type`)
    new DeserializationError(`type`, title, missingFields)
  }
  def apply(missingFields: Seq[String]): DeserializationError = {
    DeserializationError("deserialization error", missingFields)
  }
}
