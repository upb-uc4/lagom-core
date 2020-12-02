package de.upb.cs.uc4.shared.client

import play.api.libs.json.{ Format, Json }

case class JsonServiceVersion(serviceVersion: String)

object JsonServiceVersion {
  implicit val format: Format[JsonServiceVersion] = Json.format
}