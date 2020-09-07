package de.upb.cs.uc4.shared.client.exceptions

import de.upb.cs.uc4.shared.client.exceptions.ErrorType.ErrorType
import play.api.libs.json.{ Format, Json }

case class InformativeError(`type`: ErrorType, title: String, information: String) extends CustomError

object InformativeError {
  implicit val format: Format[InformativeError] = Json.format

  def apply(`type`: ErrorType, information: String): InformativeError = {
    val title = ErrorType.getTitle(`type`)
    new InformativeError(`type`, title, information)
  }
}