package de.upb.cs.uc4.authentication.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import de.upb.cs.uc4.authentication.model.AuthenticationResponse.AuthenticationResponse
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.{JsonRole, User}

/** The AuthenticationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the AuthenticationService.
  */
trait AuthenticationService extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  val pathPrefix = "/authentication-management"
  val usernameRegularExp = "[a-zA-Z][a-zA-Z0-9]+"

  /** Checks if the username and password pair exists */
  def check(username: String, password: String): ServiceCall[Seq[Role], AuthenticationResponse]

  /** Checks if the username and password pair exists */
  def getRole(username: String): ServiceCall[NotUsed, JsonRole]

  /** Sets authentication and password of a user */
  def set(): ServiceCall[User, Done]

  /** Deletes authentication and password of a user  */
  def delete(username: String): ServiceCall[NotUsed, Done]

  /** Allows POST, DELETE, OPTIONS */
  def options(): ServiceCall[NotUsed, Done]

  /** Allows GET, OPTIONS */
  def optionsGet(): ServiceCall[NotUsed, Done]

  /** Allows POST */
  def allowedMethodsPOST(): ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedMethodsGET(): ServiceCall[NotUsed, Done]

  /** Allows DELETE */
  def allowedMethodsDELETE(): ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    named("AuthenticationApi").withCalls(
      restCall(Method.POST, pathPrefix + "/users", set _),
      restCall(Method.GET, pathPrefix + "/users?username&password", check _),
      restCall(Method.GET, pathPrefix + "/users/:username/role", getRole _),
      restCall(Method.DELETE, pathPrefix + "/users/:username", delete _),
      restCall(Method.OPTIONS, pathPrefix + "/users", allowedMethodsPOST _),
      restCall(Method.OPTIONS, pathPrefix + "/users/:username/role", allowedMethodsGET _),
      restCall(Method.OPTIONS, pathPrefix + "/users/:username", allowedMethodsDELETE _)
    ).withAcls(
      ServiceAcl.forMethodAndPathRegex(Method.POST, s"\\Q$pathPrefix/users\\E"),
      ServiceAcl.forMethodAndPathRegex(Method.GET, s"\\Q$pathPrefix/users/\\E$usernameRegularExp\\Q/role\\E"),
      ServiceAcl.forMethodAndPathRegex(Method.DELETE, s"\\Q$pathPrefix/users/\\E$usernameRegularExp"),
      ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, s"\\Q$pathPrefix/users\\E"),
      ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, s"\\Q$pathPrefix/users/\\E$usernameRegularExp\\Q/role\\E"),
      ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, s"\\Q$pathPrefix/users/\\E$usernameRegularExp")
    )
  }
}
