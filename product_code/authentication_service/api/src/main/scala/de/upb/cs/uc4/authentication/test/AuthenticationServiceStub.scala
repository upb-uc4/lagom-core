package de.upb.cs.uc4.authentication.test

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser }

import scala.concurrent.Future

class AuthenticationServiceStub extends AuthenticationService {

  /** Checks if the username and password pair exists */
  override def check(user: String, pw: String): ServiceCall[NotUsed, (String, AuthenticationRole)] = ServiceCall {
    _ =>
      if (user.contains("student")) {
        Future.successful(user, AuthenticationRole.Student)
      }
      else {
        if (user.contains("lecturer")) {
          Future.successful(user, AuthenticationRole.Lecturer)
        }
        else {
          Future.successful("admin", AuthenticationRole.Admin)
        }
      }
  }

  /** Sets the authentication data of a user */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Changes the password of the given user */
  override def setAuthentication(): ServiceCall[AuthenticationUser, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Allows PUT */
  override def changePassword(username: String): ServiceCall[AuthenticationUser, Done] = ServiceCall { _ => Future.successful(Done) }

  /** This Methods needs to allow a GET-Method */
  override def allowedPut: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }
}
