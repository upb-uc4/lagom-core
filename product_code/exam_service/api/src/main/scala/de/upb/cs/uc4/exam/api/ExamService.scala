package de.upb.cs.uc4.exam.api

import de.upb.cs.uc4.shared.client._

/** The MatriculationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the MatriculationService.
  */
trait ExamService extends UC4HyperledgerService {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  override val pathPrefix: String = "/exam-management"
  /** The name of the service */
  override val name: String = "exam"
}
