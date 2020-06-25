package de.upb.cs.uc4.authentication.impl

import java.util.Base64

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.user.model.Role
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

/** Tests for the AuthenticationService
  * All tests need to be started in the defined order
  */
class AuthenticationServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new AuthenticationApplication(ctx) with LocalServiceLocator
  }

  private val client: AuthenticationService = server.serviceClient.implement[AuthenticationService]

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(user: String, pw: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString(s"$user:$pw".getBytes()))
  }

  /** Tests only working if the whole instance is started */
  "AuthenticationService service" should {

    "have the standard admin" in {
      client.check("admin", "admin").invoke(Seq(Role.Admin)).map{ answer =>
        answer shouldEqual AuthenticationResponse.Correct
      }
    }

    "have the standard lecturer" in {
      client.check("lecturer", "lecturer").invoke(Seq(Role.Lecturer)).map{ answer =>
        answer shouldEqual AuthenticationResponse.Correct
      }
    }

    "have the standard student" in {
      client.check("student", "student").invoke(Seq(Role.Student)).map{ answer =>
        answer shouldEqual AuthenticationResponse.Correct
      }
    }

    "detect a wrong username" in {
      client.check("studenta", "student").invoke(Seq(Role.Student)).map{ answer =>
        answer shouldEqual AuthenticationResponse.WrongUsername
      }
    }

    "detect a wrong password" in {
      client.check("admin", "admina").invoke(Seq(Role.Student)).map{ answer =>
        answer shouldEqual AuthenticationResponse.WrongPassword
      }
    }

    "detect that a user is not authorized" in {
      client.check("student", "student").invoke(Seq(Role.Lecturer)).map{ answer =>
        answer shouldEqual AuthenticationResponse.NotAuthorized
      }
    }
  }
}
