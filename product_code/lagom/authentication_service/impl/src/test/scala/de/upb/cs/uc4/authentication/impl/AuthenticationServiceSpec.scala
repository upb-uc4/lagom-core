package de.upb.cs.uc4.authentication.impl

import java.util.Base64

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.{Forbidden, RequestHeader}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.user.model.{JsonRole, Role}
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

  //Test users
  private val user0 = User("testStudent", "test", Role.Student)
  private val user2 = User("testLecturer", "test", Role.Lecturer)
  private val user1 = User("testAdmin", "test", Role.Admin)

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

    "be able to add a user as an admin" in {
      client.set().handleRequestHeader(addAuthorizationHeader("admin", "admin")).invoke(user0).map{ answer =>
        answer should ===(Done)
      }
    }

    "not be able to add a user as a lecturer" in {
      client.set().handleRequestHeader(addAuthorizationHeader("lecturer", "lecturer")).invoke(user1).failed
        .map{ answer =>
          answer shouldBe a [Forbidden]
      }
    }

    "not be able to add a user as a student" in {
      client.set().handleRequestHeader(addAuthorizationHeader("student", "student")).invoke(user2).failed
        .map{ answer =>
          answer shouldBe a [Forbidden]
        }
    }

    "not be able to delete a user as a student" in {
      client.delete(user0.username).handleRequestHeader(addAuthorizationHeader("student", "student"))
        .invoke().failed.map{ answer =>
          answer shouldBe a [Forbidden]
        }
    }

    "not be able to delete a user as a lecturer" in {
      client.delete(user0.username).handleRequestHeader(addAuthorizationHeader("lecturer", "lecturer"))
        .invoke().failed.map{ answer =>
        answer shouldBe a [Forbidden]
      }
    }

    "be able to delete a user as an admin" in {
      client.delete(user0.username).handleRequestHeader(addAuthorizationHeader("admin", "admin"))
        .invoke().flatMap(_ => client.check(user0.username, user0.password).invoke(Role.All)).map { answer =>
        answer shouldEqual AuthenticationResponse.WrongUsername
      }
    }

    "return the correct role for student" in {
      client.getRole("student").handleRequestHeader(addAuthorizationHeader("student", "student"))
        .invoke().map{ answer =>
        answer shouldEqual JsonRole(Role.Student)
      }
    }

    "return the correct role for lecturer" in {
      client.getRole("lecturer").handleRequestHeader(addAuthorizationHeader("lecturer", "lecturer"))
        .invoke().map{ answer =>
        answer shouldEqual JsonRole(Role.Lecturer)
      }
    }

    "return the correct role for admin" in {
      client.getRole("admin").handleRequestHeader(addAuthorizationHeader("admin", "admin"))
        .invoke().map{ answer =>
        answer shouldEqual JsonRole(Role.Admin)
      }
    }
  }
}
