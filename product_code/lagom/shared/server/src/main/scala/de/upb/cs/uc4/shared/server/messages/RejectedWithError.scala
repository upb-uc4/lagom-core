package de.upb.cs.uc4.shared.server.messages

import de.upb.cs.uc4.shared.client.exceptions.CustomError
import play.api.libs.json.{Format, Json}

/** RejectedWithError version of the Confirmation */
case class RejectedWithError(statusCode: Int, reason: CustomError) extends Confirmation

object RejectedWithError {
  implicit val format: Format[RejectedWithError] = Json.format
}