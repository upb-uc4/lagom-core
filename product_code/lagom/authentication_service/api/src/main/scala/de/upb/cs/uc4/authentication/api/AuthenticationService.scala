package de.upb.cs.uc4.authentication.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import de.upb.cs.uc4.authentication.model.AuthenticationResponse.AuthenticationResponse
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.User

/** The AuthenticationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the AuthenticationService.
  */
trait AuthenticationService extends Service {

  /** Checks if the username and password pair exists */
  def check(username: String, password: String): ServiceCall[Seq[Role], AuthenticationResponse]

  /** Sets authentication and password of a user */
  def set(): ServiceCall[User, Done]

  /** Deletes authentication and password of a user  */
  def delete(username: String): ServiceCall[NotUsed, Done]

  /** Allows GET, POST, DELETE */
  def options(): ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    named("authentication").withCalls(
      restCall(Method.POST, "/authentication", set _),
      restCall(Method.GET, "/authentication?username&password", check _),
      restCall(Method.DELETE, "/authentication?username", delete _),
      restCall(Method.OPTIONS, "/authentication", options _)
    ).withAcls(
      ServiceAcl.forMethodAndPathRegex(Method.POST, "\\Q/authentication\\E"),
      ServiceAcl.forMethodAndPathRegex(Method.DELETE, "\\Q/authentication\\E"),
      ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q/authentication\\E")
    )
  }
}