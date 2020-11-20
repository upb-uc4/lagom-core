package de.upb.cs.uc4.shared.client.exceptions

class UC4NonCriticalException(errorCode: Int, possibleErrorResponse: UC4Error, cause: Throwable = null)
  extends UC4Exception(errorCode, possibleErrorResponse, cause)
