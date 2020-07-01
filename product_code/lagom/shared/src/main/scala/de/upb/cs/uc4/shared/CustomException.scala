package de.upb.cs.uc4.shared

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode
import de.upb.cs.uc4.shared.messages.DetailedError


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
