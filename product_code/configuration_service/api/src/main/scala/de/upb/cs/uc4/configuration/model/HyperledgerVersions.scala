package de.upb.cs.uc4.configuration.model

import play.api.libs.json.{ Format, Json }

case class HyperledgerVersions(apiVersion: String, chaincodeVersion: String, networkVersion: String)

object HyperledgerVersions {
  implicit val format: Format[HyperledgerVersions] = Json.format
}