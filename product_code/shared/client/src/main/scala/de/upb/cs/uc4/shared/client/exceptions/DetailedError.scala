package de.upb.cs.uc4.shared.client.exceptions

import play.api.libs.json._

case class DetailedError(`type`: String, title: String, invalidParams: Seq[SimpleError]) extends CustomError

object DetailedError {
  implicit val format: Format[DetailedError] = Json.format

  def apply(`type`: String, invalidParams: Seq[SimpleError]): DetailedError = {
    val title = CustomError.getTitle(`type`)
    new DetailedError(`type`, title, invalidParams)
  }
}

