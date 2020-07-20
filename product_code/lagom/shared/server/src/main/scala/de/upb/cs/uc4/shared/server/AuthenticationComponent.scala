package de.upb.cs.uc4.shared.server

import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import de.upb.cs.uc4.authentication.api.AuthenticationService

/** Mixin to include into a Service, to enable authenticated ServiceCalls  */
trait AuthenticationComponent extends LagomServiceClientComponents  { self: LagomConfigComponent =>
  lazy implicit val authenticationService: AuthenticationService = serviceClient.implement[AuthenticationService]
}
