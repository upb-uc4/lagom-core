package de.upb.cs.uc4.operation.impl.events

import play.api.libs.json.{ Format, Json }

case class OnRemoveFromWatchlist(operationId: String) extends OperationEvent

object OnRemoveFromWatchlist {
  implicit val format: Format[OnRemoveFromWatchlist] = Json.format
}
