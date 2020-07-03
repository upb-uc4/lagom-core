package de.upb.cs.uc4.shared.api

import play.api.libs.json._

case class DetailedError(`type`: String, title: String, errors: Seq[SimpleError])

object DetailedError {
  implicit val format: Format[DetailedError] = Json.format

  implicit def optionFormat[T: Format]: Format[Option[T]] = new Format[Option[T]]{
    override def reads(json: JsValue): JsResult[Option[T]] = json.validateOpt[T]

    override def writes(o: Option[T]): JsValue = o match {
      case Some(t) ⇒ implicitly[Writes[T]].writes(t)
      case None ⇒ JsNull
    }
  }
}

