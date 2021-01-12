package de.upb.cs.uc4.operation.impl.events

import play.api.libs.json.{ Format, Json }

case class OnAddToWatchlist(operationId: String) extends OperationEvent

object OnAddToWatchlist {
  implicit val format: Format[OnAddToWatchlist] = Json.format
}