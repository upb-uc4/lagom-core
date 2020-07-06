package de.upb.cs.uc4.shared.server.messages

import play.api.libs.json.{Format, Json}

/** Rejected version of the Confirmation */
case class Rejected(reason: String) extends Confirmation

object Rejected {
  implicit val format: Format[Rejected] = Json.format
}
