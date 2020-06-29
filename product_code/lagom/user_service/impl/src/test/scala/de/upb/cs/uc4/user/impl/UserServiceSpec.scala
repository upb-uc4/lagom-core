package de.upb.cs.uc4.user.impl

import java.util.Base64

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{NotFound, RequestHeader, TransportException}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.post.{PostMessageAdmin, PostMessageLecturer, PostMessageStudent}
import de.upb.cs.uc4.user.model.user.{Admin, AuthenticationUser, Lecturer, Student}
import de.upb.cs.uc4.user.model.{Address, Role}
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

        override def login(): ServiceCall[NotUsed, String] = ServiceCall{ _ => Future.successful("")}

        override def logout(): ServiceCall[NotUsed, Done] = ServiceCall{ _ => Future.successful(Done)}

        override def check(jws: String): ServiceCall[NotUsed, (String, AuthenticationRole)] =
          ServiceCall{ _ => Future.successful("admin", AuthenticationRole.Admin)}

        override def getRole(username: String): ServiceCall[NotUsed, AuthenticationRole] =
          ServiceCall{ _ => Future.successful(AuthenticationRole.Admin)}
      }
    }
  }
  val client: UserService = server.serviceClient.implement[UserService]

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Bearer MOCK")
  }

  //Test users
  val address: Address = Address("Deppenstraße", "42a", "1337", "Entenhausen", "Nimmerland")
  val authenticationUser: AuthenticationUser = AuthenticationUser("MOCK", "MOCK", AuthenticationRole.Admin)

  val student0: Student = Student("student0", Role.Student, address, "Hans", "Wurst", "Haesslich", "hans.wurst@mail.de", "IN", "421769", 9000, List())
  val lecturer0: Lecturer = Lecturer("lecturer0", Role.Lecturer, address, "Graf", "Wurst", "Haesslich", "graf.wurst@mail.de", "Ich bin bloed", "Genderstudies")
  val admin0: Admin = Admin("admin0", Role.Lecturer, address, "Dieter", "Wurst", "Haesslich", "dieter.wurst@mail.de")
  val admin1: Admin = Admin("lecturer0", Role.Lecturer, address, "Lola", "Wurst", "Haesslich", "lola.wurst@mail.de")

  /** Tests only working if the whole instance is started */
  "UserService service" should {

    "get all users with no users" in {
      client.getAllUsers.handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
        answer.admins shouldBe empty
        answer.lecturer shouldBe empty
        answer.students shouldBe empty
      }
    }

    "add a student" in {
      client.addStudent().handleRequestHeader(addAuthorizationHeader()).invoke(PostMessageStudent(authenticationUser, student0)).map { answer =>
        answer.errors shouldBe empty
      }
    }

    "add a lecturer" in {
      client.addLecturer().handleRequestHeader(addAuthorizationHeader()).invoke(PostMessageLecturer(authenticationUser, lecturer0)).map { answer =>
        answer.errors shouldBe empty
      }
    }

    "add an admin" in {
      client.addAdmin().handleRequestHeader(addAuthorizationHeader()).invoke(PostMessageAdmin(authenticationUser, admin0)).map { answer =>
        answer.errors shouldBe empty
      }
    }

    "delete a non-existing user" in {
      client.deleteUser("WurstAG").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }

    "find a non-existing student" in {
      client.getStudent("WurstAG").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { answer =>
        answer shouldBe a[NotFound]
      }
    }

    "find a non-existing lecturer" in {
      client.getLecturer("WurstAG").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { answer =>
        answer shouldBe a[NotFound]
      }
    }

    "find a non-existing admin" in {
      client.getAdmin("WurstAG").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { answer =>
        answer shouldBe a[NotFound]
      }
    }

    "update a non-existing student" in {
      client.updateStudent(student0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(student0.copy(username = "Guten Abend")).failed.map { answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }

    "update a non-existing lecturer" in {
      client.updateLecturer(lecturer0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(lecturer0.copy(username = "Guten Abend")).failed.map { answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }

    "update a non-existing admin" in {
      client.updateAdmin(admin0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(admin0.copy(username = "Guten Abend")).failed.map { answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }

    "add an already existing user" in {
      client.addAdmin().handleRequestHeader(addAuthorizationHeader())
        .invoke(PostMessageAdmin(authenticationUser, admin1)).failed.map { answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(409)
      }
    }

    "delete a user" in {
      client.deleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
        client.getStudent(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().failed
      }.map { answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }

    "update a user" in {
      client.updateAdmin(admin0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(admin0.copy(firstName = "KLAUS")).flatMap { _ =>
        client.getAdmin(admin0.username).handleRequestHeader(addAuthorizationHeader()).invoke()
      }.map { answer =>
        answer.firstName shouldBe "KLAUS"
      }
    }

    "get a role of a user" in {
      client.getRole(lecturer0.username).handleRequestHeader(addAuthorizationHeader()).invoke().map{ answer =>
        answer.role shouldBe Role.Lecturer
      }
    }
  }
}
