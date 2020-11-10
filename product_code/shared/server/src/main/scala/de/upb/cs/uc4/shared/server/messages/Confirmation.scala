package de.upb.cs.uc4.shared.server.messages

import de.upb.cs.uc4.shared.client.exceptions.UC4Error
import play.api.libs.json.{ Format, JsResult, JsValue, Json }

/** Used as return value if a command was executed successfully */
sealed trait Confirmation

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

/** Accepted version of the Confirmation */
final case class Accepted(summary: String) extends Confirmation

object Accepted {
  val default: Accepted = Accepted("The command was successful")

  implicit val format: Format[Accepted] = Json.format
}

/** Rejected version of the Confirmation */
final case class Rejected(statusCode: Int, reason: UC4Error) extends Confirmation

object Rejected {
  implicit def format: Format[Rejected] = Json.format
}
