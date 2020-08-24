package de.upb.cs.uc4.authentication.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceAcl, ServiceCall }
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.UC4Service
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

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
  def check(token: String): ServiceCall[NotUsed, (String, AuthenticationRole)]

  /** Sets the authentication data of a user */
  def setAuthentication(): ServiceCall[AuthenticationUser, Done]

  /** Changes the password of the given user */
  def changePassword(username: String): ServiceCall[AuthenticationUser, Done]

  /** Logins a user and return a refresh and a login token in the header */
  def login: ServiceCall[NotUsed, Done]

  /** Generates a new login token out of a refresh token */
  def refresh: ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows PUT */
  def allowedPut: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/users?token", check _),
        restCall(Method.POST, pathPrefix + "/users", setAuthentication _),

        restCall(Method.PUT, pathPrefix + "/users/:username", changePassword _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.GET, pathPrefix + "/login", login _),
        restCall(Method.GET, pathPrefix + "/refresh", refresh _),

        restCall(Method.OPTIONS, pathPrefix + "/users", allowedPut _),
        restCall(Method.OPTIONS, pathPrefix + "/login", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/refresh", allowedGet _)
      )
      .addAcls(
        ServiceAcl.forMethodAndPathRegex(Method.PUT, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/login\\E"),
        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/refresh\\E"),

        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/login\\E"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/refresh\\E")
      )
  }
}
