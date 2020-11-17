package de.upb.cs.uc4.examreg.model

import play.api.libs.json.{ Format, Json }

case class Module(id: String, name: String)

object Module {
  implicit val format: Format[Module] = Json.format
}