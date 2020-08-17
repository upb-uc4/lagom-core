package de.upb.cs.uc4.shared.client.exceptions

import play.api.libs.json._

case class GenericError(`type`: String, title: String) extends CustomError

object GenericError {
  implicit val format: Format[GenericError] = Json.format

  def apply(`type`: String): GenericError = {
    val title = CustomError.getTitle(`type`)
    new GenericError(`type`, title)
  }
}
