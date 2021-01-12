package de.upb.cs.uc4.operation.impl.actor

import play.api.libs.json.{ Format, Json }

case class WatchlistWrapper(watchlist: Seq[String]){

  def addedToList(operationId: String): WatchlistWrapper ={
    WatchlistWrapper((watchlist :+ operationId).distinct)
  }

  def removedFromList(operationId: String): WatchlistWrapper ={
    WatchlistWrapper(watchlist.filter(_ != operationId))
  }
}

object WatchlistWrapper {
  implicit val format: Format[WatchlistWrapper] = Json.format
}

