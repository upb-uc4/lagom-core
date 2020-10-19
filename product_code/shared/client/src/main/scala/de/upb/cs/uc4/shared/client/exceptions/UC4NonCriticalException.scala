package de.upb.cs.uc4.shared.client.exceptions

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

class UC4NonCriticalException(errorCode: TransportErrorCode, possibleErrorResponse: UC4Error, cause: Throwable)
  extends UC4Exception(errorCode, possibleErrorResponse, cause) {

  def this(errorCode: TransportErrorCode, possibleErrorResponse: UC4Error) =
    this(errorCode, possibleErrorResponse, null)

  def this(errorCode: Int, possibleErrorResponse: UC4Error) =
    this(TransportErrorCode(errorCode, 1003, "Error"), possibleErrorResponse)
}
