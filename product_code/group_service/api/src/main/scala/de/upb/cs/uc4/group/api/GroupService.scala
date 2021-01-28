package de.upb.cs.uc4.group.api

import de.upb.cs.uc4.shared.client._

/** The GroupService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the GroupService.
  */
trait GroupService extends UC4HyperledgerService {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  override val pathPrefix: String = "/group-management"
  /** The name of the service */
  override val name: String = "group"
}
