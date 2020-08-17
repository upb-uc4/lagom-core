package de.upb.cs.uc4.user.impl.events

import play.api.libs.json.{ Format, Json }

case class OnLatestMatriculationUpdate(semester: String) extends UserEvent

object OnLatestMatriculationUpdate {
  implicit val format: Format[OnLatestMatriculationUpdate] = Json.format
}