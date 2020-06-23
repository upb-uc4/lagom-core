package de.upb.cs.uc4.user.impl

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.authentication.model.AuthenticationResponse.AuthenticationResponse
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.user.Admin
import de.upb.cs.uc4.user.model.{JsonRole, Role}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Future

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class UserServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new UserApplication(ctx) with LocalServiceLocator {
      override lazy val authenticationService: AuthenticationService = new AuthenticationService {
        /** Checks if the username and password pair exists */
        override def check(username: String, password: String): ServiceCall[Seq[Role], AuthenticationResponse] =
          ServiceCall { _ =>  Future.successful(AuthenticationResponse.Correct)}

        /** Gets the role of a user */
        override def getRole(username: String): ServiceCall[NotUsed, JsonRole] =
          ServiceCall { _ =>  Future.successful(JsonRole(Role.Admin))}

        /** Sets authentication and password of a user */
        override def set(): ServiceCall[Admin, Done] =
          ServiceCall { _ =>  Future.successful(Done)}

        /** Deletes authentication and password of a user  */
        override def delete(username: String): ServiceCall[NotUsed, Done] =
          ServiceCall { _ =>  Future.successful(Done)}

        /** Allows GET, POST, DELETE, OPTIONS*/
        override def options(): ServiceCall[NotUsed, Done] =
          ServiceCall { _ =>  Future.successful(Done)}

        /** Allows GET, OPTIONS */
        override def optionsGet(): ServiceCall[NotUsed, Done] =
          ServiceCall { _ =>  Future.successful(Done)}

        /** Allows POST */
        override def allowedMethodsPOST(): ServiceCall[NotUsed, Done] = ServiceCall { _ =>  Future.successful(Done)}

        /** Allows GET */
        override def allowedMethodsGET(): ServiceCall[NotUsed, Done] = ServiceCall { _ =>  Future.successful(Done)}

        /** Allows DELETE */
        override def allowedMethodsDELETE(): ServiceCall[NotUsed, Done] = ServiceCall { _ =>  Future.successful(Done)}
      }
    }
  }
}
