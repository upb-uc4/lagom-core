package de.upb.cs.uc4.shared.server.messages

import de.upb.cs.uc4.shared.client.exceptions.UC4Error
import play.api.libs.json.{ Format, Json }

/** Rejected version of the Confirmation */
case class Rejected(statusCode: Int, reason: UC4Error) extends Confirmation

object Rejected {
  implicit val format: Format[Rejected] = Json.format
}