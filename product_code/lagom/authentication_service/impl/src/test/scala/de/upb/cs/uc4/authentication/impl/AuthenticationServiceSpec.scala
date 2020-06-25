package de.upb.cs.uc4.authentication.impl

import java.util.Base64

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ProducerStub, ProducerStubFactory, ServiceTest}
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.post.{PostMessageAdmin, PostMessageLecturer, PostMessageStudent}
import de.upb.cs.uc4.user.model.user.{Admin, AuthenticationUser, Lecturer, Student}
import de.upb.cs.uc4.user.model.{GetAllUsersResponse, JsonRole, JsonUsername, Role}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future

/** Tests for the AuthenticationService
  * All tests need to be started in the defined order
  */
class AuthenticationServiceSpec extends AsyncWordSpec
  with Matchers with BeforeAndAfterAll with Eventually with ScalaFutures {

  private var authenticationStub: ProducerStub[AuthenticationUser] = _
  private var deletionStub: ProducerStub[JsonUsername] = _

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new AuthenticationApplication(ctx) with LocalServiceLocator {
      // Declaration as lazy values forces right execution order
      lazy val stubFactory = new ProducerStubFactory(actorSystem, materializer)
      lazy val internAuthenticationStub: ProducerStub[AuthenticationUser] =
        stubFactory.producer[AuthenticationUser](UserService.AUTHENTICATION_TOPIC_NAME)
      lazy val internDeletionStub: ProducerStub[JsonUsername] =
        stubFactory.producer[JsonUsername](UserService.DELETE_TOPIC_NAME)

      authenticationStub = internAuthenticationStub
      deletionStub = internDeletionStub

      // Create a userService with ProducerStub as topic
      override lazy val userService: UserServiceStub = new UserServiceStub(internAuthenticationStub, internDeletionStub)
    }
  }

  private val client: AuthenticationService = server.serviceClient.implement[AuthenticationService]

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(user: String, pw: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString(s"$user:$pw".getBytes()))
  }

  /** Tests only working if the whole instance is started */
  "AuthenticationService service" should {

    "have the standard admin" in {
      client.check("admin", "admin").invoke(Seq(Role.Admin)).map { answer =>
        answer shouldEqual AuthenticationResponse.Correct
      }
    }

    "have the standard lecturer" in {
      client.check("lecturer", "lecturer").invoke(Seq(Role.Lecturer)).map { answer =>
        answer shouldEqual AuthenticationResponse.Correct
      }
    }

    "have the standard student" in {
      client.check("student", "student").invoke(Seq(Role.Student)).map { answer =>
        answer shouldEqual AuthenticationResponse.Correct
      }
    }

    "detect a wrong username" in {
      client.check("studenta", "student").invoke(Seq(Role.Student)).map { answer =>
        answer shouldEqual AuthenticationResponse.WrongUsername
      }
    }

    "detect a wrong password" in {
      client.check("admin", "admina").invoke(Seq(Role.Student)).map { answer =>
        answer shouldEqual AuthenticationResponse.WrongPassword
      }
    }

    "detect that a user is not authorized" in {
      client.check("student", "student").invoke(Seq(Role.Lecturer)).map { answer =>
        answer shouldEqual AuthenticationResponse.NotAuthorized
      }
    }

    "add a new user over the topic" in {
      authenticationStub.send(new AuthenticationUser("Ben", "Hermann", Role.Admin))

      eventually(timeout(Span(5, Seconds))) {
        val futureAnswer = client.check("Ben", "Hermann").invoke(Seq(Role.Admin))
        whenReady(futureAnswer) { answer =>
          answer shouldEqual AuthenticationResponse.Correct
        }
      }
    }

    "remove a user over the topic" in {
      deletionStub.send(JsonUsername("admin"))

      eventually(timeout(Span(5, Seconds))) {
        val futureAnswer = client.check("admin", "admin").invoke(Seq(Role.Admin))
        whenReady(futureAnswer) { answer =>
          answer shouldEqual AuthenticationResponse.WrongUsername
        }
      }
    }
  }
}

class UserServiceStub(authenticationStub: ProducerStub[AuthenticationUser],
                      deletionStub: ProducerStub[JsonUsername]) extends UserService {

  override def getAllUsers: ServiceCall[NotUsed, GetAllUsersResponse] = ServiceCall { _ => Future.successful(null) }

  override def deleteUser(username: String): ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAllStudents: ServiceCall[NotUsed, Seq[Student]] = ServiceCall { _ => Future.successful(Seq()) }

  override def addStudent(): ServiceCall[PostMessageStudent, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getStudent(username: String): ServiceCall[NotUsed, Student] = ServiceCall { _ => Future.successful(null) }

  override def updateStudent(username: String): ServiceCall[Student, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAllLecturers: ServiceCall[NotUsed, Seq[Lecturer]] = ServiceCall { _ => Future.successful(Seq()) }

  override def addLecturer(): ServiceCall[PostMessageLecturer, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getLecturer(username: String): ServiceCall[NotUsed, Lecturer] = ServiceCall { _ => Future.successful(null) }

  override def updateLecturer(username: String): ServiceCall[Lecturer, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAllAdmins: ServiceCall[NotUsed, Seq[Admin]] = ServiceCall { _ => Future.successful(Seq()) }

  override def addAdmin(): ServiceCall[PostMessageAdmin, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAdmin(username: String): ServiceCall[NotUsed, Admin] = ServiceCall { _ => Future.successful(null) }

  override def updateAdmin(username: String): ServiceCall[Admin, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] = ServiceCall { _ => Future.successful(null) }

  override def allowedGetPut: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedGetPost: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedDelete: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def userAuthenticationTopic(): Topic[AuthenticationUser] = authenticationStub.topic

  override def userDeletedTopic(): Topic[JsonUsername] = deletionStub.topic
}
