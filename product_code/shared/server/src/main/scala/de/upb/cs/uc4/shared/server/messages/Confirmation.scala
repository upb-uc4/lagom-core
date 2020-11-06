package de.upb.cs.uc4.shared.server.messages

import play.api.libs.json.{ Format, JsResult, JsValue, Json }

/** Used as return value if a command was executed successfully */
trait Confirmation

/** Handles json parsing */
case object Confirmation {
  implicit val format: Format[Confirmation] = new Format[Confirmation] {
    override def reads(json: JsValue): JsResult[Confirmation] = json match {
      case json if (json \ "statusCode").isDefined => Json.fromJson[Rejected](json)
      case json => Json.fromJson[Accepted](json)
    }

    override def writes(o: Confirmation): JsValue = {
      o match {
        case acc: Accepted => Json.toJson(acc)
        case rej: Rejected => Json.toJson(rej)
      }
    }
  }
}
