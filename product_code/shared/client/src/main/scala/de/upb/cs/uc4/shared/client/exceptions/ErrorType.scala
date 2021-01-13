package de.upb.cs.uc4.shared.client.exceptions

import play.api.libs.json.{ Format, Json }

object ErrorType extends Enumeration {
  type ErrorType = Value
  val NotModified, //304
  Deserialization, KafkaDeserialization, JsonValidation, AlreadyDeleted, MalformedRefreshToken, MalformedLoginToken, UnexpectedEntity, MultipleAuthorization, //400
  MissingHeader, QueryParameter, //In an DetailedError
  BasicAuthorization, JwtAuthorization, RefreshTokenExpired, LoginTokenExpired, RefreshTokenMissing, //401
  NotEnoughPrivileges, OwnerMismatch, //403
  KeyNotFound, NotEnrolled, //404
  AlreadyEnrolled, KeyDuplicate, RemovalNotAllowed, //409
  EntityTooLarge, //413
  UnsupportedMediaType, //415
  Teapot, //418
  PathParameterMismatch, RefreshTokenSignatureInvalid, LoginTokenSignatureInvalid, ValidationTimeout, //422
  Validation, UneditableFields, //422 In a DetailedError
  PreconditionRequired, //428
  InternalServer, UndeserializableException, //500
  NotImplemented, //501
  HLInternal, //In an InformativeError
  HLUnknownTransactionId, HLInsufficientApprovals, HLNotFound, HLConflict, HLUnprocessableLedgerState, //In a GenericError
  HLUnprocessableEntity, HLInvalidEntity, //In a DetailedError
  HLInvalidTransactionCall //In a TransactionError
  = Value

  implicit val format: Format[ErrorType] = Json.formatEnum(this)

  def All: Seq[ErrorType] = values.toSeq

  def getTitle(errorType: ErrorType): String = {
    errorType match {
      //HL errors are missing, but as they are given to us with a title, we do not need to find a fitting title
      //If not stated otherwise, the type is contained in a  GenericError
      //304
      case NotModified => "The requested resource has not been modified"
      //400
      case Deserialization => "Syntax of the provided json object was incorrect"
      case JsonValidation => "The provided json object did not validate" //In a DetailedError
      case AlreadyDeleted => "The resource was already deleted"
      case MalformedRefreshToken => "The long term token is malformed"
      case MalformedLoginToken => "The login token is malformed"
      case UnexpectedEntity => "Expected another entity" //In an InformativeError
      case MultipleAuthorization => "Multiple authorization given"
      case MissingHeader => "Missing required header" //In a DetailedError
      case QueryParameter => "QueryParameter(s) invalid" //In a DetailedError
      //401
      case BasicAuthorization => "Username and password combination does not exist"
      case JwtAuthorization => "Authorization token missing"
      case RefreshTokenMissing => "Refresh token missing"
      case RefreshTokenExpired => "Your long term session expired"
      case LoginTokenExpired => "Your session expired"
      //403
      case NotEnoughPrivileges => "Insufficient privileges for this action"
      case OwnerMismatch => "You are not allowed to modify the resource"
      //404
      case KeyNotFound => "Key value is not in use"
      case NotEnrolled => "Enrollment of the user is required before the requested resource can be fetched"
      //409
      case KeyDuplicate => "Key is already in use"
      case AlreadyEnrolled => "You are already enrolled"
      case RemovalNotAllowed => "Resource cannot be removed in the current state"
      //413
      case EntityTooLarge => "Entity is too large"
      //415
      case UnsupportedMediaType => "The payload format is in an unsupported format"
      //418
      case Teapot => "I'm a teapot"
      //422
      case PathParameterMismatch => "Parameter specified in path and in object do not match"
      case Validation => "Your request parameters did not validate" //In a DetailedError
      case ValidationTimeout => "Validation of the given object timed out"
      case UneditableFields => "Attempted to change uneditable fields" //In a DetailedError
      case RefreshTokenSignatureInvalid => "The long term token has a wrong signature"
      case LoginTokenSignatureInvalid => "The login term token has a wrong signature"
      //428
      case PreconditionRequired => "A required precondition for this call is not fulfilled"
      //500
      case InternalServer => "An internal server error has occurred"
      case UndeserializableException => "Internal error while deserializing Exception"
      case HLInternal => "Hyperledger encountered an internal error" //In an InformativeError
      //501
      case NotImplemented => "The invoked method is not implemented yet"

      //Hopefully never
      case _ => "Undefined error title, please open a bug report at https://github.com/upb-uc4/lagom-core/issues/new?assignees=&labels=bug&template=bug_report.md&title="
    }
  }

}
