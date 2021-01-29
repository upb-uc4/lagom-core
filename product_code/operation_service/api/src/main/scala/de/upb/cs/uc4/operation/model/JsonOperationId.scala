package de.upb.cs.uc4.operation.model

import play.api.libs.json.{ Format, Json }

case class JsonOperationId(id: String)

object JsonOperationId {
  implicit val format: Format[JsonOperationId] = Json.format
}
