package de.upb.cs.uc4.user.model

import play.api.libs.json.{ Format, Json }

case class MatriculationUpdate(username: String, semester: String)

object MatriculationUpdate {
  implicit val format: Format[MatriculationUpdate] = Json.format
}
