package de.upb.cs.uc4.shared.client.exceptions

import de.upb.cs.uc4.shared.client.exceptions.ErrorType.ErrorType
import play.api.libs.json._

case class GenericError(`type`: ErrorType, title: String) extends UC4Error

object GenericError {
  implicit val format: Format[GenericError] = Json.format

  def apply(`type`: ErrorType): GenericError = {
    val title = ErrorType.getTitle(`type`)
    new GenericError(`type`, title)
  }
}
