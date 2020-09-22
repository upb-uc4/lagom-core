package de.upb.cs.uc4.certificate.model

import play.api.libs.json.{ Format, Json }

case class JsonCertificate(certificate: String)

object JsonCertificate {
  implicit val format: Format[JsonCertificate] = Json.format
}
