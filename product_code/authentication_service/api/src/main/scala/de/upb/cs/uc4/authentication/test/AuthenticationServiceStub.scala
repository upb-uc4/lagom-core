package de.upb.cs.uc4.authentication.test

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser }

import scala.concurrent.Future

class AuthenticationServiceStub extends AuthenticationService {

  /** Checks if the username and password pair exists */
  override def check(token: String): ServiceCall[NotUsed, (String, AuthenticationRole)] = ServiceCall {
    _ =>
      if (token.contains("student")) {
        Future.successful(token, AuthenticationRole.Student)
      }
      else {
        if (token.contains("lecturer")) {
          Future.successful(token, AuthenticationRole.Lecturer)
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

  /** Logins a user and return a refresh and a login token in the header */
  override def login: ServiceCall[NotUsed, Done] = ???

  /** Generates a new login token out of a refresh token */
  override def refresh: ServiceCall[NotUsed, Done] = ???

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = ???
}
