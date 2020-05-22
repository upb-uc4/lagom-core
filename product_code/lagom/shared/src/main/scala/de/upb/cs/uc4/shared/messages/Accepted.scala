package de.upb.cs.uc4.shared.messages

import play.api.libs.json._

trait Accepted extends Confirmation

case object Accepted extends Accepted {
  implicit val format: Format[Accepted] =
    Format(Reads(_ => JsSuccess(Accepted)), Writes(_ => Json.obj()))
}
