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
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent }
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, Role }
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success, Try }

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

  def addAuthorizationHeader(username: String = "admin"): RequestHeader => RequestHeader = { header =>

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

  def prepare(users: Seq[User]): Future[Seq[User]] = {
    val createdUsers = users.map { user =>
      val newUsername = user.username
      val postMessage = user match {
        case s: Student  => PostMessageStudent(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Student), s.copy(username = newUsername))
        case l: Lecturer => PostMessageLecturer(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Lecturer), l.copy(username = newUsername))
        case a: Admin    => PostMessageAdmin(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Admin), a.copy(username = newUsername))
      }
      Await.result(client.addUser().handleRequestHeader(addAuthorizationHeader("admin")).invoke(postMessage), 5.seconds)
    }
    eventually(timeout(Span(2, Minutes))) {
      checkUserCreation(createdUsers)
    }.map { _ => createdUsers }
  }

  def checkUserCreation(users: Seq[User]): Future[Assertion] = {
    for {
      studentList <- if (users.exists(_.role == Role.Student)) {
        client.getAllStudents(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
      }
      else {
        Future.successful(Seq())
      }
      lecturerList <- if (users.exists(_.role == Role.Lecturer)) {
        client.getAllLecturers(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
      }
      else {
        Future.successful(Seq())
      }
      adminList <- if (users.exists(_.role == Role.Admin)) {
        client.getAllAdmins(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
      }
      else {
        Future.successful(Seq())
      }
    } yield {
      val userList: Seq[User] = studentList ++ lecturerList ++ adminList
      userList should contain allElementsOf users
    }
  }

  def resetUserDatabase(): Future[Assertion] = {
    //Start deletion process
    resetUserTable(Role.Student)
    resetUserTable(Role.Lecturer)
    resetUserTable(Role.Admin)

    //Check for 15 seconds, if the deletion was successful
    eventually(timeout(Span(15, Seconds))) {

      for {
        usernamesStudent <- server.application.database.getAll(Role.Student)
        usernamesLecturer <- server.application.database.getAll(Role.Lecturer)
        usernamesAdmin <- server.application.database.getAll(Role.Admin)
      } yield {
        usernamesStudent ++ usernamesLecturer ++ usernamesAdmin should contain theSameElementsAs Seq("student", "lecturer", "admin")
      }
    }
  }

  def resetUserTable(role: Role): Future[Seq[Done]] = {
    val roleString = role.toString.toLowerCase
    server.application.database.getAll(role).map(
      _.filter(_ != roleString).map { username =>
        val result = Await.result(client.deleteUser(username).handleRequestHeader(addAuthorizationHeader()).invoke(), 5.seconds)
        assert(result == Done)
        result
      }
    )
  }

  def cleanup[Assertion](): PartialFunction[Try[Assertion], Future[Assertion]] = PartialFunction.fromFunction {
    case Success(value) =>
      resetUserDatabase().map { _ =>
        value
      }
    case Failure(throwable) =>
      resetUserDatabase().map { _ =>
        throw throwable
      }
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
      eventually(timeout(Span(15, Seconds))) {
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
      eventually(timeout(Span(15, Seconds))) {
        client.getAllStudents(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
          answer should contain(student0)
        }
      }.andThen(cleanup())
    }

    "fail on adding a user with different username in authUser" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(PostMessageAdmin(admin0Auth.copy(username = admin0.username + "changed"), admin0))
        .failed.map {
          answer => answer.asInstanceOf[CustomException].getErrorCode.http should ===(422)
        }.andThen(cleanup())
    }

    "fail on adding an already existing User" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.addUser().handleRequestHeader(addAuthorizationHeader("admin"))
          .invoke(PostMessageAdmin(admin0Auth, admin0.copy(firstName = "Dieter"))).failed.flatMap { answer =>
            answer.asInstanceOf[CustomException].getErrorCode.http should ===(409)
          }
      }.andThen(cleanup())
    }

    //GET TESTS
    "fetch the information of a User as an Admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { answer =>
          answer should ===(student0)
        }
      }.andThen(cleanup())
    }

    "fetch the information of a User as the User (non-Admin) himself" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().flatMap { answer =>

          answer should ===(student0)

        }
      }.andThen(cleanup())
    }

    "fetch the public information of a User as another User (non-Admin)" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>

          answer should ===(student0.toPublic)

        }
      }.andThen(cleanup())
    }

    "fetch the public information of all specified Students, as a non-Admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getAllStudents(Some(student0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>

          answer should contain theSameElementsAs Seq(student0.toPublic)

        }
      }.andThen(cleanup())
    }
    "fetch the public information of all specified Lecturers, as a non-Admin" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.getAllLecturers(Some(lecturer0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>

          answer should contain theSameElementsAs Seq(lecturer0.toPublic)

        }
      }.andThen(cleanup())
    }
    "fetch the public information of all specified Admins, as a non-Admin" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.getAllAdmins(Some(admin0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>

          answer should contain theSameElementsAs Seq(admin0.toPublic)

        }
      }.andThen(cleanup())
    }
    "fetch the information of all specified Students, as an Admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getAllStudents(Some(student0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { answer =>

          answer should contain theSameElementsAs Seq(student0)

        }
      }.andThen(cleanup())
    }
    "fetch the information of all specified Lecturers, as an Admin" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.getAllLecturers(Some(lecturer0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { answer =>

          answer should contain theSameElementsAs Seq(lecturer0)

        }
      }.andThen(cleanup())
    }
    "fetch the information of all specified Admins, as an Admin" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.getAllAdmins(Some(admin0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(admin0)
        }
      }.andThen(cleanup())
    }
    "fetch the public information of all specified Users, as a non-Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>

          answer should ===(GetAllUsersResponse(Seq(student0.toPublic), Seq(lecturer0.toPublic), Seq(admin0.toPublic)))

        }
      }.andThen(cleanup())
    }
    "fetch the information of all specified Users, as an Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { answer =>

          answer should ===(GetAllUsersResponse(Seq(student0), Seq(lecturer0), Seq(admin0)))

        }
      }.andThen(cleanup())
    }
    "find a non-existing User" in {
      client.getUser("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "get the role of a user" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.getRole(lecturer0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { answer =>

          answer.role shouldBe Role.Lecturer

        }
      }.andThen(cleanup())
    }

    //UPDATE TESTS
    "not update a non-existing User" in {
      client.updateUser("GutenAbend").handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(student0.copy(username = "GutenAbend")).failed.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
        }
    }

    "update a user as an admin" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.updateUser(admin0.username).handleRequestHeader(addAuthorizationHeader("admin"))
          .invoke(admin0.copy(firstName = "KLAUS")).flatMap { _ =>
            client.getUser(admin0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
          }.flatMap { answer =>

            answer.firstName shouldBe "KLAUS"

          }
      }.andThen(cleanup())
    }

    "update a user as the user himself" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.updateUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username))
          .invoke(lecturer0Updated).flatMap { _ =>
            client.getUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
          }.flatMap { answer =>

            answer should ===(lecturer0Updated)

          }
      }.andThen(cleanup())
    }

    "not update uneditable fields as an admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.updateUser(student0UpdatedUneditable.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedUneditable.username))
          .invoke(student0UpdatedUneditable).failed.flatMap { answer =>

            answer.asInstanceOf[CustomException].getPossibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
              have length uneditableErrorSize

          }
      }.andThen(cleanup())
    }

    "not update protected fields as the user himself" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.updateUser(student0UpdatedProtected.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedProtected.username))
          .invoke(student0UpdatedProtected).failed.flatMap { answer =>

            answer.asInstanceOf[CustomException].getPossibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
              have length protectedErrorSize

          }
      }.andThen(cleanup())
    }

    //DELETE TESTS
    "delete a non-existing user" in {
      client.deleteUser("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed.map {
        answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }
    "delete a user" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.deleteUser(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { _ =>
          client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed
        }.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
        }
      }.andThen(cleanup())
    }
  }
}
