package de.upb.cs.uc4.shared.messages

import play.api.libs.json.{Format, Json}

case class PossibleErrorResponse(errors: Seq[DetailedError])

object PossibleErrorResponse {
  implicit val format: Format[PossibleErrorResponse] = Json.format
}

