package de.upb.cs.uc4.shared.client.exceptions

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

class UC4Exception(
    val errorCode: TransportErrorCode,
    val possibleErrorResponse: UC4Error,
    val cause: Throwable
)
  extends Exception(possibleErrorResponse.title, cause, true, true) {

  def this(errorCode: TransportErrorCode, possibleErrorResponse: UC4Error) =
    this(errorCode, possibleErrorResponse, null)

  def this(errorCode: Int, possibleErrorResponse: UC4Error) =
    this(TransportErrorCode(errorCode, 1003, "Error"), possibleErrorResponse)
}

object UC4Exception {
  //400
  val DeserializationError = new UC4Exception(400, GenericError(ErrorType.Deserialization))
  val MalformedRefreshToken = new UC4Exception(400, GenericError(ErrorType.MalformedRefreshToken))
  val MalformedLoginToken = new UC4Exception(400, GenericError(ErrorType.MalformedLoginToken))
  val MultipleAuthorizationError = new UC4Exception(400, GenericError(ErrorType.MultipleAuthorization))
  //401
  val JwtAuthorizationError = new UC4Exception(401, GenericError(ErrorType.JwtAuthorization))
  val RefreshTokenMissing = new UC4Exception(401, GenericError(ErrorType.RefreshTokenMissing))
  val BasicAuthorizationError = new UC4Exception(401, GenericError(ErrorType.BasicAuthorization))
  val RefreshTokenExpired = new UC4Exception(401, GenericError(ErrorType.RefreshTokenExpired))
  val LoginTokenExpired = new UC4Exception(401, GenericError(ErrorType.LoginTokenExpired))
  //403
  val NotEnoughPrivileges = new UC4Exception(403, GenericError(ErrorType.NotEnoughPrivileges))
  val OwnerMismatch = new UC4Exception(403, GenericError(ErrorType.OwnerMismatch))
  //404
  val NotFound = new UC4Exception(404, GenericError(ErrorType.KeyNotFound))
  val NotEnrolled = new UC4Exception(404, GenericError(ErrorType.NotEnrolled))
  //409
  val Duplicate = new UC4Exception(409, GenericError(ErrorType.KeyDuplicate))
  val AlreadyEnrolled = new UC4Exception(409, GenericError(ErrorType.AlreadyEnrolled))
  //415
  val UnsupportedMediaType = new UC4Exception(415, GenericError(ErrorType.UnsupportedMediaType))
  //422
  val ValidationTimeout = new UC4Exception(422, GenericError(ErrorType.ValidationTimeout))
  val PathParameterMismatch = new UC4Exception(422, GenericError(ErrorType.PathParameterMismatch))
  val RefreshTokenSignatureError = new UC4Exception(422, GenericError(ErrorType.RefreshTokenSignatureInvalid))
  val LoginTokenSignatureError = new UC4Exception(422, GenericError(ErrorType.LoginTokenSignatureInvalid))
  //500
  val InternalServerError = new UC4Exception(500, GenericError(ErrorType.InternalServer))
  val InternalDeserializationError = new UC4Exception(500, GenericError(ErrorType.UndeserializableException))

  def InternalServerError(name: String, reason: String) =
    new UC4Exception(500, DetailedError(ErrorType.InternalServer, Seq(SimpleError(name, reason))))

  def InternalServerError(invalidParams: SimpleError*) =
    new UC4Exception(500, DetailedError(ErrorType.InternalServer, invalidParams))
}
