package de.upb.cs.uc4.shared.client.exceptions

import de.upb.cs.uc4.shared.client.exceptions.ErrorType.ErrorType
import play.api.libs.json._

case class DetailedError(`type`: ErrorType, title: String, invalidParams: Seq[SimpleError]) extends UC4Error

object DetailedError {
  implicit val format: Format[DetailedError] = Json.format

  def apply(`type`: ErrorType, invalidParams: Seq[SimpleError]): DetailedError = {
    val title = ErrorType.getTitle(`type`)
    new DetailedError(`type`, title, invalidParams)
  }
}

