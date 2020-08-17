package de.upb.cs.uc4.shared.server

import com.lightbend.lagom.scaladsl.client.ServiceClient
import de.upb.cs.uc4.authentication.api.AuthenticationService

/** Mixin to include into a Service, to enable authenticated ServiceCalls  */
trait AuthenticationComponent {
  val serviceClient: ServiceClient
  lazy implicit val authenticationService: AuthenticationService = serviceClient.implement[AuthenticationService]
}
