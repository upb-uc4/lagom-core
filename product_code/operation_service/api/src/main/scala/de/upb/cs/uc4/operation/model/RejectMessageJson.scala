package de.upb.cs.uc4.operation.model

import play.api.libs.json.{ Format, Json }

case class RejectMessageJson(rejectMessage: String)

object RejectMessageJson {
  implicit val format: Format[RejectMessageJson] = Json.format
}