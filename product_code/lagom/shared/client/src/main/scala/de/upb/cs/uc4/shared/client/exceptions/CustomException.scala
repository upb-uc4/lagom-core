package de.upb.cs.uc4.shared.client.exceptions

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode


class CustomException(errorCode: TransportErrorCode, possibleErrorResponse: CustomError, cause: Throwable) extends Exception(possibleErrorResponse.title, null, true, true){

  def this(errorCode: TransportErrorCode, possibleErrorResponse: CustomError) =
    this(errorCode, possibleErrorResponse, null)


  def getErrorCode: TransportErrorCode = {
    errorCode
  }
  def getPossibleErrorResponse: CustomError = {
    possibleErrorResponse
  }
}
