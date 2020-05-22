package de.upb.cs.uc4.shared.messages

import play.api.libs.json.{Format, Json}

case class Rejected(reason: String) extends Confirmation

object Rejected {
  implicit val format: Format[Rejected] = Json.format
}
