package de.upb.cs.uc4.shared

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode
import de.upb.cs.uc4.shared.messages.PossibleErrorResponse


class CustomException(errorCode: TransportErrorCode, possibleErrorResponse: PossibleErrorResponse, cause: Throwable) extends Exception(possibleErrorResponse.title, null, true, false){

  def this(errorCode: TransportErrorCode, possibleErrorResponse: PossibleErrorResponse) =
    this(errorCode, possibleErrorResponse, null)


  def getErrorCode: TransportErrorCode = {
    errorCode
  }
  def getPossibleErrorResponse: PossibleErrorResponse = {
    possibleErrorResponse
  }
}
