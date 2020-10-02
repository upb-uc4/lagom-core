package de.upb.cs.uc4.shared.server.messages

import de.upb.cs.uc4.shared.client.exceptions.UC4Error
import play.api.libs.json.{ Format, Json }

/** RejectedWithError version of the Confirmation */
case class RejectedWithError(statusCode: Int, reason: UC4Error) extends Confirmation

object RejectedWithError {
  implicit val format: Format[RejectedWithError] = Json.format
}