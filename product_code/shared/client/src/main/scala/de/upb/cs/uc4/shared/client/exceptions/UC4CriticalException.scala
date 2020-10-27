package de.upb.cs.uc4.shared.client.exceptions

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

class UC4CriticalException(errorCode: TransportErrorCode, possibleErrorResponse: UC4Error, cause: Throwable)
  extends UC4Exception(errorCode, possibleErrorResponse, cause) {

  def this(errorCode: Int, possibleErrorResponse: UC4Error, cause: Throwable = null) =
    this(TransportErrorCode(errorCode, 1003, "Error"), possibleErrorResponse, cause)
}
