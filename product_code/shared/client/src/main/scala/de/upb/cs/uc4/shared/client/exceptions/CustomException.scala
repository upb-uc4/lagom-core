package de.upb.cs.uc4.shared.client.exceptions

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

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
  val DeserializationError = new CustomException(400, GenericError("deserialization error"))
  val MalformedRefreshToken = new CustomException(400, GenericError("malformed refresh token"))
  val MalformedLoginToken = new CustomException(400, GenericError("malformed login token"))
  //401
  val AuthorizationError = new CustomException(401, GenericError("jwt authorization error"))
  val BasicAuthorizationError = new CustomException(401, GenericError("basic authorization error"))
  val RefreshTokenExpired = new CustomException(401, GenericError("refresh token expired"))
  val LoginTokenExpired = new CustomException(401, GenericError("login token expired"))
  //403
  val NotEnoughPrivileges = new CustomException(403, GenericError("not enough privileges"))
  val OwnerMismatch = new CustomException(403, GenericError("owner mismatch"))
  //404
  val NotFound = new CustomException(404, GenericError("key not found"))
  //409
  val Duplicate = new CustomException(409, GenericError("key duplicate"))
  //422
  val PathParameterMismatch = new CustomException(422, GenericError("path parameter mismatch"))
  val RefreshTokenSignatureError = new CustomException(422, GenericError("refresh token signature invalid"))
  val LoginTokenSignatureError = new CustomException(422, GenericError("login token signature invalid"))
  //500
  val InternalServerError = new CustomException(500, GenericError("internal server error"))
  val InternalDeserializationError = new CustomException(500, GenericError("undeserializable exception"))
}
