package de.upb.cs.uc4.shared.messages

import play.api.libs.json._

case class PossibleErrorResponse(`type`: String, title: String, errors: Seq[DetailedError])

object PossibleErrorResponse {
  implicit val format: Format[PossibleErrorResponse] = Json.format

  implicit def optionFormat[T: Format]: Format[Option[T]] = new Format[Option[T]]{
    override def reads(json: JsValue): JsResult[Option[T]] = json.validateOpt[T]

    override def writes(o: Option[T]): JsValue = o match {
      case Some(t) ⇒ implicitly[Writes[T]].writes(t)
      case None ⇒ JsNull
    }
  }
}

