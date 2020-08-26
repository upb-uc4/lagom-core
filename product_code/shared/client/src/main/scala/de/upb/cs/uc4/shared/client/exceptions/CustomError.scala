package de.upb.cs.uc4.shared.client.exceptions

import play.api.libs.json.{ Format, JsResult, JsValue, Json }

trait CustomError {
  val `type`: String
  val title: String

}

object CustomError {
  implicit val format: Format[CustomError] = new Format[CustomError] {
    override def reads(json: JsValue): JsResult[CustomError] = json match {
      case json if (json \ "invalidParams").isDefined => Json.fromJson[DetailedError](json)
      case json if (json \ "transactionId").isDefined => Json.fromJson[TransactionError](json)
      case json if (json \ "information").isDefined => Json.fromJson[InformativeError](json)
      case json => Json.fromJson[GenericError](json)
    }

    override def writes(o: CustomError): JsValue = o match {
      case dErr: DetailedError    => Json.toJson(dErr)
      case gErr: GenericError     => Json.toJson(gErr)
      case tErr: TransactionError => Json.toJson(tErr)
      case iErr: InformativeError => Json.toJson(iErr)
    }
  }

  def getTitle(`type`: String): String = {
    `type` match {
      //HL errors are missing, but as they are given to us with a title, we do not need to find a fitting title
      //If not stated otherwise, the type is contained in a  GenericError
      //400
      case "deserialization error" => "Syntax of the provided json object was incorrect"
      case "json validation error" => "The provided json object did not validate"
      case "wrong object" => "Received object differs from expected object" // InformativeError
      case "malformed refresh token" => "The long term token is malformed"
      case "malformed login token" => "The login token is malformed"
      //401
      case "basic authorization error" => "Username and password combination does not exist"
      case "jwt authorization error" => "Authorization token missing"
      case "refresh token expired" => "Your long term session expired"
      case "login token expired" => "Your session expired"
      //403
      case "not enough privileges" => "Insufficient privileges for this action"
      case "owner mismatch" => "You are not allowed to modify the resource"
      //404
      case "key not found" => "Key value is not in use"
      //409
      case "key duplicate" => "Key is already in use"
      //418
      case "teapot" => "I'm a teapot"
      //422
      case "path parameter mismatch" => "Parameter specified in path and in object do not match"
      case "validation error" => "Your request parameters did not validate" //In a DetailedError
      case "uneditable fields" => "Attempted to change uneditable fields" //In a DetailedError
      case "refresh token signature invalid" => "The long term token has a wrong signature"
      case "login token signature invalid" => "The login term token has a wrong signature"
      //500
      case "internal server error" => "An internal server error has occurred"
      case "undeserializable exception" => "Internal error while deserializing Exception"
      case "hl: internal error" => "Hyperledger encountered an internal error" //In an InformativeError
      //???
      case _ => "Title not Found"

    }
  }
}
