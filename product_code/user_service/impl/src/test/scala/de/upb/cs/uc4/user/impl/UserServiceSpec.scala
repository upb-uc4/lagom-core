package de.upb.cs.uc4.user.impl

import java.util.Calendar

import akka.Done
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.authentication.AuthenticationServiceStub
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser, JsonUsername }
import de.upb.cs.uc4.shared.client.exceptions.{ CustomException, DetailedError }
import de.upb.cs.uc4.user.DefaultTestUsers
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent }
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, Role }
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Span }
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class UserServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually with DefaultTestUsers {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new UserApplication(ctx) with LocalServiceLocator with TestTopicComponents {
        override lazy val authentication: AuthenticationService = new AuthenticationServiceStub()
      }
    }

  val client: UserService = server.serviceClient.implement[UserService]

  val deletionTopic: Source[JsonUsername, _] = client.userDeletedTopic().subscribe.atMostOnceSource

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(username: String): RequestHeader => RequestHeader = { header =>

    var role = AuthenticationRole.Admin

    if (username.contains("student")) {
      role = AuthenticationRole.Student
    }
    else if (username.contains("lecturer")) {
      role = AuthenticationRole.Lecturer
    }

    val time = Calendar.getInstance()
    time.add(Calendar.DATE, 1)

    val token =
      Jwts.builder()
        .setSubject("login")
        .setExpiration(time.getTime)
        .claim("username", username)
        .claim("authenticationRole", role.toString)
        .signWith(SignatureAlgorithm.HS256, "changeme")
        .compact()

    header.withHeader("Cookie", s"login=$token")
  }

  def prepare(users: Seq[User]): Unit = {
    users.foreach { user =>
      val postMessage = user match {
        case s: Student  => PostMessageStudent(AuthenticationUser(s.username, s.username, AuthenticationRole.Student), s)
        case l: Lecturer => PostMessageLecturer(AuthenticationUser(l.username, l.username, AuthenticationRole.Lecturer), l)
        case a: Admin    => PostMessageAdmin(AuthenticationUser(a.username, a.username, AuthenticationRole.Admin), a)
      }
      assert(Await.result(client.addUser().handleRequestHeader(addAuthorizationHeader("admin")).invoke(postMessage), 5.seconds).isInstanceOf[User])
    }
    eventually(timeout(Span(2, Minutes))) {
      client.getAllUsers(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        val userList: Seq[User] = answer.students ++ answer.lecturers ++ answer.admins
        assert(users.forall(userList.contains(_)))
      }
    }
  }

  def cleanup(users: Seq[User]): Unit = {
    users.foreach {
      user =>
        assert(Await.result(client.deleteUser(user.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke(), 5.seconds).isInstanceOf[Done])
    }
    eventually(timeout(Span(2, Minutes))) {
      client.getAllUsers(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        assert(checkUserDeletion(users))
      }
    }
  }

  def checkUserDeletion(users: Seq[User]): Boolean = {
    val searchList = users.map(user => user.username)

    Await.result(
      for {
        studentList <- server.application.database.getAll(Role.Student)
        lecturerList <- server.application.database.getAll(Role.Lecturer)
        adminList <- server.application.database.getAll(Role.Admin)
      } yield {
        val allUsernames = studentList ++ lecturerList ++ adminList
        searchList.forall(!allUsernames.contains(_))
      },
      5.seconds
    )

  }

  //Additional variables needed for some tests
  val student0UpdatedUneditable: Student = student0.copy(latestImmatriculation = "SS2012")
  val student0UpdatedProtected: Student = student0UpdatedUneditable.copy(firstName = "Dieter", lastName = "Dietrich", birthDate = "1996-12-11", matriculationId = "1333337")
  val uneditableErrorSize: Int = 1
  val protectedErrorSize: Int = 4 + uneditableErrorSize

  val lecturer0Updated: Lecturer = lecturer0.copy(picture = "aBetterPicture", email = "noreply@scam.ng", address = address1, freeText = "Morgen kommt der große Gauss.", researchArea = "Physics")

  /** Tests only working if the whole instance is started */
  "UserService service" should {

    "get all users with default users" in {
      eventually(timeout(Span(2, Minutes))) {
        client.getAllUsers(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>

          answer.admins should have size 1
          answer.lecturers should have size 1
          answer.students should have size 1
        }
      }
    }

    //ADD TESTS
    "add a student" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin")).invoke(PostMessageStudent(student0Auth, student0))
      eventually(timeout(Span(2, Minutes))) {
        client.getAllStudents(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
          cleanup(Seq(student0))
          answer should contain(student0)
        }
      }
    }

    "fail on adding a user with different username in authUser" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(PostMessageAdmin(admin0Auth.copy(username = admin0.username + "changed"), admin0))
        .failed.map {
          answer => answer.asInstanceOf[CustomException].getErrorCode.http should ===(422)
        }
    }

    "fail on adding an already existing User" in {
      prepare(Seq(admin0))
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(PostMessageAdmin(admin0Auth, admin0.copy(firstName = "Dieter"))).failed.map { answer =>
          cleanup(Seq(admin0))
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(409)
        }
    }

    //GET TESTS
    "fetch the information of a User as an Admin" in {
      prepare(Seq(student0))
      client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        cleanup(Seq(student0))
        answer should ===(student0)
      }
    }

    "fetch the information of a User as the User (non-Admin) himself" in {
      prepare(Seq(student0))
      client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map { answer =>
        cleanup(Seq(student0))
        answer should ===(student0)
      }
    }

    "fetch the public information of a User as another User (non-Admin)" in {
      client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader("student")).invoke().map { answer =>
        answer should ===(student0.toPublic)
      }
    }

    "fetch the public information of all specified Students, as a non-Admin" in {
      client.getAllStudents(Some(student0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().map { answer =>
        answer should contain theSameElementsAs Seq(student0.toPublic)
      }
    }
    "fetch the public information of all specified Lecturers, as a non-Admin" in {
      client.getAllLecturers(Some(lecturer0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().map { answer =>
        answer should contain theSameElementsAs Seq(lecturer0.toPublic)
      }
    }
    "fetch the public information of all specified Admins, as a non-Admin" in {
      client.getAllAdmins(Some(admin0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().map { answer =>
        answer should contain theSameElementsAs Seq(admin0.toPublic)
      }
    }
    "fetch the information of all specified Students, as an Admin" in {
      client.getAllStudents(Some(student0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer should contain theSameElementsAs Seq(student0)
      }
    }
    "fetch the information of all specified Lecturers, as an Admin" in {
      client.getAllLecturers(Some(lecturer0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer should contain theSameElementsAs Seq(lecturer0)
      }
    }
    "fetch the information of all specified Admins, as an Admin" in {
      client.getAllAdmins(Some(admin0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer should contain theSameElementsAs Seq(admin0)
      }
    }
    "fetch the public information of all specified Users, as a non-Admin" in {
      client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().map { answer =>
        answer should ===(GetAllUsersResponse(Seq(student0.toPublic), Seq(lecturer0.toPublic), Seq(admin0.toPublic)))
      }
    }
    "fetch the information of all specified Users, as an Admin" in {
      client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer should ===(GetAllUsersResponse(Seq(student0), Seq(lecturer0), Seq(admin0)))
      }
    }
    "find a non-existing User" in {
      client.getUser("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "get the role of a user" in {
      client.getRole(lecturer0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer.role shouldBe Role.Lecturer
      }
    }

    //UPDATE TESTS
    "not update a non-existing User" in {
      client.updateUser("GutenAbend").handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(student0.copy(username = "GutenAbend")).failed.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
        }
    }

    "update a user as an admin" in {
      client.updateUser(admin0.username).handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(admin0.copy(firstName = "KLAUS")).flatMap { _ =>
          client.getUser(admin0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
        }.map { answer =>
          answer.firstName shouldBe "KLAUS"
        }
    }

    "update a user as the user himself" in {
      client.updateUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username))
        .invoke(lecturer0Updated).flatMap { _ =>
          client.getUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
        }.map { answer =>
          answer should ===(lecturer0Updated)
        }
    }

    "not update uneditable fields as an admin" in {
      client.updateUser(student0UpdatedUneditable.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedUneditable.username))
        .invoke(student0UpdatedUneditable).failed.map { answer =>
          answer.asInstanceOf[CustomException].getPossibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
            have length uneditableErrorSize
        }
    }

    "not update protected fields as the user himself" in {
      client.updateUser(student0UpdatedProtected.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedProtected.username))
        .invoke(student0UpdatedProtected).failed.map { answer =>
          answer.asInstanceOf[CustomException].getPossibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
            have length protectedErrorSize
        }
    }

    //DELETE TESTS
    "delete a non-existing user" in {
      client.deleteUser("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed.map {
        answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }
    "delete a user" in {
      client.deleteUser(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed
      }.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

  }
}
