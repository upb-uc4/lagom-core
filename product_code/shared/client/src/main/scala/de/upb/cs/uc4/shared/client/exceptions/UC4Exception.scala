package de.upb.cs.uc4.shared.client.exceptions

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

abstract class UC4Exception(
    val errorCode: TransportErrorCode,
    val possibleErrorResponse: UC4Error,
    val cause: Throwable
) extends Exception(possibleErrorResponse.title, cause, true, true) {

  override def getMessage: String = super.getMessage + "\n" + possibleErrorResponse.toString
}

object UC4Exception {

  def apply(errorCode: Int, possibleErrorResponse: UC4Error, cause: Throwable = null): UC4Exception = {
    if (errorCode >= 500) {
      new UC4CriticalException(errorCode, possibleErrorResponse, cause)
    }
    else {
      new UC4NonCriticalException(errorCode, possibleErrorResponse)
    }
  }

  //400
  val DeserializationError = new UC4NonCriticalException(400, GenericError(ErrorType.Deserialization))
  val MalformedRefreshToken = new UC4NonCriticalException(400, GenericError(ErrorType.MalformedRefreshToken))
  val MalformedLoginToken = new UC4NonCriticalException(400, GenericError(ErrorType.MalformedLoginToken))
  val MultipleAuthorizationError = new UC4NonCriticalException(400, GenericError(ErrorType.MultipleAuthorization))
  def QueryParameterError(invalidParams: SimpleError*) =
    new UC4NonCriticalException(400, DetailedError(ErrorType.QueryParameter, invalidParams))
  //401
  val JwtAuthorizationError = new UC4NonCriticalException(401, GenericError(ErrorType.JwtAuthorization))
  val RefreshTokenMissing = new UC4NonCriticalException(401, GenericError(ErrorType.RefreshTokenMissing))
  val BasicAuthorizationError = new UC4NonCriticalException(401, GenericError(ErrorType.BasicAuthorization))
  val RefreshTokenExpired = new UC4NonCriticalException(401, GenericError(ErrorType.RefreshTokenExpired))
  val LoginTokenExpired = new UC4NonCriticalException(401, GenericError(ErrorType.LoginTokenExpired))
  //403
  val NotEnoughPrivileges = new UC4NonCriticalException(403, GenericError(ErrorType.NotEnoughPrivileges))
  val OwnerMismatch = new UC4NonCriticalException(403, GenericError(ErrorType.OwnerMismatch))
  //404
  val NotFound = new UC4NonCriticalException(404, GenericError(ErrorType.KeyNotFound))
  val NotEnrolled = new UC4NonCriticalException(404, GenericError(ErrorType.NotEnrolled))
  //409
  val Duplicate = new UC4NonCriticalException(409, GenericError(ErrorType.KeyDuplicate))
  val AlreadyEnrolled = new UC4NonCriticalException(409, GenericError(ErrorType.AlreadyEnrolled))
  //415
  val UnsupportedMediaType = new UC4NonCriticalException(415, GenericError(ErrorType.UnsupportedMediaType))
  //422
  val ValidationTimeout = new UC4NonCriticalException(422, GenericError(ErrorType.ValidationTimeout))
  val PathParameterMismatch = new UC4NonCriticalException(422, GenericError(ErrorType.PathParameterMismatch))
  val RefreshTokenSignatureError = new UC4NonCriticalException(422, GenericError(ErrorType.RefreshTokenSignatureInvalid))
  val LoginTokenSignatureError = new UC4NonCriticalException(422, GenericError(ErrorType.LoginTokenSignatureInvalid))
  //500
  val InternalDeserializationError = new UC4CriticalException(500, GenericError(ErrorType.UndeserializableException), null)

  def InternalServerError(name: String, reason: String, throwable: Throwable = null) =
    new UC4CriticalException(500, DetailedError(ErrorType.InternalServer, Seq(SimpleError(name, reason))), throwable)

  def InternalServerError(throwable: Throwable, invalidParams: SimpleError*) =
    new UC4CriticalException(500, DetailedError(ErrorType.InternalServer, invalidParams), throwable)

  //501
  val NotImplemented = new UC4NonCriticalException(501, GenericError(ErrorType.NotImplemented))
}
