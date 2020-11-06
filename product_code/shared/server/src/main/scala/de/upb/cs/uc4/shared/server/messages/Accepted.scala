package de.upb.cs.uc4.shared.server.messages

import play.api.libs.json._

/** Accepted version of the Confirmation */
case class Accepted(summary: String) extends Confirmation

object Accepted {
  val default: Accepted = Accepted("The command was successful")

  implicit val format: Format[Accepted] = Json.format
}
