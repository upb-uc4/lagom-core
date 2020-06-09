package de.upb.cs.uc4.shared

import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import de.upb.cs.uc4.authentication.api.AuthenticationService

trait AuthenticationComponent extends LagomServiceClientComponents  { self: LagomConfigComponent =>
  lazy val authenticationService: AuthenticationService = serviceClient.implement[AuthenticationService]
}
