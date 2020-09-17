package de.upb.cs.uc4.shared.client.exceptions

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode
import play.api.libs.json.{ Format, JsValue, Json, Writes }

class CustomException(errorCode: TransportErrorCode, possibleErrorResponse: CustomError, cause: Throwable) extends Exception(possibleErrorResponse.title, null, true, true) {

  def this(errorCode: TransportErrorCode, possibleErrorResponse: CustomError) =
    this(errorCode, possibleErrorResponse, null)

  def this(errorCode: Int, possibleErrorResponse: CustomError) =
    this(TransportErrorCode(errorCode, 1003, "Error"), possibleErrorResponse)

  def getErrorCode: TransportErrorCode = {
    errorCode
  }

  def getPossibleErrorResponse: CustomError = {
    possibleErrorResponse
  }
}
object CustomException {
  //400
  val DeserializationError = new CustomException(400, GenericError(ErrorType.Deserialization))
  val MalformedRefreshToken = new CustomException(400, GenericError(ErrorType.MalformedRefreshToken))
  val MalformedLoginToken = new CustomException(400, GenericError(ErrorType.MalformedLoginToken))
  val MultipleAuthorizationError = new CustomException(400, GenericError(ErrorType.MultipleAuthorization))
  //401
  val AuthorizationError = new CustomException(401, GenericError(ErrorType.JwtAuthorization))
  val BasicAuthorizationError = new CustomException(401, GenericError(ErrorType.BasicAuthorization))
  val RefreshTokenExpired = new CustomException(401, GenericError(ErrorType.RefreshTokenExpired))
  val LoginTokenExpired = new CustomException(401, GenericError(ErrorType.LoginTokenExpired))
  //403
  val NotEnoughPrivileges = new CustomException(403, GenericError(ErrorType.NotEnoughPrivileges))
  val OwnerMismatch = new CustomException(403, GenericError(ErrorType.OwnerMismatch))
  //404
  val NotFound = new CustomException(404, GenericError(ErrorType.KeyNotFound))
  //409
  val Duplicate = new CustomException(409, GenericError(ErrorType.KeyDuplicate))
  //422
  val PathParameterMismatch = new CustomException(422, GenericError(ErrorType.PathParameterMismatch))
  val RefreshTokenSignatureError = new CustomException(422, GenericError(ErrorType.RefreshTokenSignatureInvalid))
  val LoginTokenSignatureError = new CustomException(422, GenericError(ErrorType.LoginTokenSignatureInvalid))
  //500
  val InternalServerError = new CustomException(500, GenericError(ErrorType.InternalServer))
  val InternalDeserializationError = new CustomException(500, GenericError(ErrorType.UndeserializableException))

  implicit val writes: Writes[CustomException] = (o: CustomException) => Json.toJson(o.getPossibleErrorResponse)
}
