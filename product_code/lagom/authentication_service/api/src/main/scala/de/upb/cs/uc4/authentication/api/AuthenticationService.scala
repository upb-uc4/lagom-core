package de.upb.cs.uc4.authentication.api

import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.upb.cs.uc4.authentication.model.AuthenticationResponse.AuthenticationResponse
import de.upb.cs.uc4.user.model.Role.Role

/** The AuthenticationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the AuthenticationService.
  */
trait AuthenticationService extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  val pathPrefix = "/authentication-management"

  /** Checks if the username and password pair exists */
  def check(username: String, password: String): ServiceCall[Seq[Role], AuthenticationResponse]

  final override def descriptor: Descriptor = {
    import Service._
    named("authentication").withCalls(
      restCall(Method.GET, pathPrefix + "/users?username&password", check _)
    )
  }
}
