package de.upb.cs.uc4.authentication.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.UC4Service

/** The AuthenticationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the AuthenticationService.
  */
trait AuthenticationService extends UC4Service {
  override val pathPrefix: String = "/authentication-management"
  override val name: String = "authentication"
  override val autoAcl: Boolean = false

  /** Checks if the username and password pair exists */
  def check(user: String, pw: String): ServiceCall[NotUsed, (String, AuthenticationRole)]

  /** Sets the authentication data of a user */
  def setAuthentication(): ServiceCall[AuthenticationUser, Done]

  /** Changes the password of the given user */
  def changePassword(username: String): ServiceCall[AuthenticationUser, Done]

  /** Allows PUT */
  def allowedPut: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/users?user&pw", check _),
        restCall(Method.POST, pathPrefix + "/users", setAuthentication _),
        restCall(Method.PUT, pathPrefix + "/users/:username", changePassword _),
        restCall(Method.OPTIONS, pathPrefix + "/users", allowedPut _),
      )
      .addAcls(
        ServiceAcl.forMethodAndPathRegex(Method.PUT, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
      )
  }
}
