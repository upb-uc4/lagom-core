package de.upb.cs.uc4.shared.client.exceptions

import play.api.libs.json.{ Format, Json }

case class InformativeError(`type`: String, title: String, information: String) extends CustomError

object InformativeError {
  implicit val format: Format[InformativeError] = Json.format

  def apply(`type`: String, information: String): InformativeError = {
    val title = CustomError.getTitle(`type`)
    new InformativeError(`type`, title, information)
  }
}