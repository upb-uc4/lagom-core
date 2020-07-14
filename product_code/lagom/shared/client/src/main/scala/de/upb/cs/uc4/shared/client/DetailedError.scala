package de.upb.cs.uc4.shared.client

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

  def apply(`type`: String, errors: Seq[SimpleError]) = {
    val title = getTitle(`type`)
    new DetailedError(`type`, title, errors)
  }
  def getTitle(`type` : String) : String = {
    `type` match{
      //400
      case "path parameter mismatch" => "Parameter specified in path and in object do not match"
      case "wrong object" => "Unexpected object"
      //401
      case "authorization error" => "Username and password combination does not exist"
      //403
      case "owner mismatch" => "Only admins and the owner of the resource are allowed to modify the resource"
      //404
      case "key not found" => "Key value is not in use"
      //409
      case "key duplicate" => "Key is already in use"
      //422
      case "validation error" => "Your request parameters did not validate"

      //???
      case _ => "Server Error"
      
    }
  }
}

