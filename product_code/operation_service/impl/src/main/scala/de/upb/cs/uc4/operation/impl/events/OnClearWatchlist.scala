package de.upb.cs.uc4.operation.impl.events

import play.api.libs.json.{ Format, Json }

case class OnClearWatchlist(username: String) extends OperationEvent

object OnClearWatchlist {
  implicit val format: Format[OnClearWatchlist] = Json.format
}
