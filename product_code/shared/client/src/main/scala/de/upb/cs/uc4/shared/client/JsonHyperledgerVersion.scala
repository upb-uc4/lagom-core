package de.upb.cs.uc4.shared.client

import play.api.libs.json.{ Format, Json }

case class JsonHyperledgerVersion(hlfApiVersion: String, chaincodeVersion: String)

object JsonHyperledgerVersion {
  implicit val format: Format[JsonHyperledgerVersion] = Json.format
}
