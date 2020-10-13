package de.upb.cs.uc4.certificate.model

import play.api.libs.json.{ Format, Json }

case class JsonEnrollmentId(id: String)

object JsonEnrollmentId {
  implicit val format: Format[JsonEnrollmentId] = Json.format
}