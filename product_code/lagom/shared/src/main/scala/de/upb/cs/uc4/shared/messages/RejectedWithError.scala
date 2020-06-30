package de.upb.cs.uc4.shared.messages

import play.api.libs.json.{Format, Json}

/** RejectedWithError version of the Confirmation */
case class RejectedWithError(statusCode: Int, reason: PossibleErrorResponse) extends Confirmation

object RejectedWithError {
  implicit val format: Format[RejectedWithError] = Json.format
}