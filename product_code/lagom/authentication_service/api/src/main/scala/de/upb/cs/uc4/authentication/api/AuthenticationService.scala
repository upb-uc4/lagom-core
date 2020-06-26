package de.upb.cs.uc4.authentication.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole

/** The AuthenticationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the AuthenticationService.
  */
trait AuthenticationService extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  val pathPrefix = "/authentication-management"

  /** Logs the user in */
  def login(): ServiceCall[NotUsed, String]

  /** Logs the user out */
  def logout(): ServiceCall[NotUsed, Done]

  /** Checks if the username and password pair exists */
  def check(jws: String): ServiceCall[NotUsed, (String, AuthenticationRole)]

  /** Returns role of the given user */
  def getRole(username: String): ServiceCall[NotUsed, AuthenticationRole]

  final override def descriptor: Descriptor = {
    import Service._
    named("authentication")
      .withCalls(
        restCall(Method.GET, pathPrefix + "/login", login _),
        restCall(Method.GET, pathPrefix + "/logout", logout _),
        restCall(Method.GET, pathPrefix, check _),
        restCall(Method.GET, pathPrefix + "/users/:jws", check _),
        restCall(Method.GET, pathPrefix + "/role/:username", getRole _)
      )
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.GET, s"\\Q$pathPrefix/login\\E"),
        //ServiceAcl.forMethodAndPathRegex(Method.GET, s"\\Q$pathPrefix/logout\\E"),
      )
  }
}
