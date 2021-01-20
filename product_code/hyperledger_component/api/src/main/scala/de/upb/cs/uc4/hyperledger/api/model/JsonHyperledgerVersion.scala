package de.upb.cs.uc4.hyperledger.api.model

import play.api.libs.json.{ Format, Json }

case class JsonHyperledgerVersion(hlfApiVersion: String, chaincodeVersion: String)

object JsonHyperledgerVersion {
  implicit val format: Format[JsonHyperledgerVersion] = Json.format
}
