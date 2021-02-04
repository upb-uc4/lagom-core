package de.upb.cs.uc4.configuration.model

import play.api.libs.json.{ Format, Json }

case class JsonHyperledgerNetworkVersion(networkVersion: String)

object JsonHyperledgerNetworkVersion {
  implicit val format: Format[JsonHyperledgerNetworkVersion] = Json.format
}
