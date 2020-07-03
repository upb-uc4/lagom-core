package de.upb.cs.uc4.shared.api

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode


class CustomException(errorCode: TransportErrorCode, possibleErrorResponse: DetailedError, cause: Throwable) extends Exception(possibleErrorResponse.title, null, true, true){

  def this(errorCode: TransportErrorCode, possibleErrorResponse: DetailedError) =
    this(errorCode, possibleErrorResponse, null)


  def getErrorCode: TransportErrorCode = {
    errorCode
  }
  def getPossibleErrorResponse: DetailedError = {
    possibleErrorResponse
  }
}
