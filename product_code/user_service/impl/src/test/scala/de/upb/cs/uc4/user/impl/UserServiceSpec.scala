package de.upb.cs.uc4.user.impl

import java.util.Calendar

import akka.Done
import akka.stream.scaladsl.Source
import com.google.common.io.ByteStreams
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.authentication.AuthenticationServiceStub
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser, JsonUsername }
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.user.DefaultTestUsers
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent }
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, Role }
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll }
import play.api.test.Helpers.{ DELETE, GET, PUT, contentAsBytes, contentAsJson, route }
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.Future
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

  def createLoginToken(username: String = "admin"): String = {

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

    s"login=$token"
  }

  def addAuthorizationHeader(username: String = "admin"): RequestHeader => RequestHeader =
    header => header.withHeader("Cookie", createLoginToken(username))

  def prepare(users: Seq[User]): Future[Seq[User]] = {
    Future.sequence(users.map { user =>
      val newUsername = user.username
      val postMessage = user match {
        case s: Student  => PostMessageStudent(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Student), s.copy(username = newUsername))
        case l: Lecturer => PostMessageLecturer(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Lecturer), l.copy(username = newUsername))
        case a: Admin    => PostMessageAdmin(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Admin), a.copy(username = newUsername))
      }
      client.addUser().handleRequestHeader(addAuthorizationHeader()).invoke(postMessage)
    }).flatMap { createdUsers =>
      eventually(timeout(Span(30, Seconds))) {
        checkUserCreation(createdUsers)
      }.map { _ => createdUsers }
    }
  }

  def checkUserCreation(users: Seq[User]): Future[Assertion] = {
    for {
      studentList <- if (users.exists(_.role == Role.Student)) {
        client.getAllStudents(None).handleRequestHeader(addAuthorizationHeader()).invoke()
      }
      else {
        Future.successful(Seq())
      }
      lecturerList <- if (users.exists(_.role == Role.Lecturer)) {
        client.getAllLecturers(None).handleRequestHeader(addAuthorizationHeader()).invoke()
      }
      else {
        Future.successful(Seq())
      }
      adminList <- if (users.exists(_.role == Role.Admin)) {
        client.getAllAdmins(None).handleRequestHeader(addAuthorizationHeader()).invoke()
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
    val a = for {
      _ <- resetUserTable(Role.Student)
      _ <- resetUserTable(Role.Lecturer)
      _ <- resetUserTable(Role.Admin)
    } yield {
      eventually(timeout(Span(60, Seconds))) {
        for {
          usernamesStudent <- server.application.database.getAll(Role.Student)
          usernamesLecturer <- server.application.database.getAll(Role.Lecturer)
          usernamesAdmin <- server.application.database.getAll(Role.Admin)
        } yield {
          usernamesStudent ++ usernamesLecturer ++ usernamesAdmin should contain theSameElementsAs Seq("student", "lecturer", "admin")
        }
      }
    }
    a.flatMap(result => result)
  }

  def resetUserTable(role: Role): Future[Seq[Done]] = {
    // When role is set to Role.Student, we don't want to delete the default student with username "student"
    // For Lecturer and Admin as well
    val roleString = role.toString.toLowerCase
    val futureUserList: Future[Seq[User]] = role match {
      case Role.Student  => client.getAllStudents(None).handleRequestHeader(addAuthorizationHeader()).invoke()
      case Role.Lecturer => client.getAllLecturers(None).handleRequestHeader(addAuthorizationHeader()).invoke()
      case Role.Admin    => client.getAllAdmins(None).handleRequestHeader(addAuthorizationHeader()).invoke()
    }
    futureUserList.map {
      _.filter(_.username != roleString).map { user =>
        client.deleteUser(user.username).handleRequestHeader(addAuthorizationHeader()).invoke()
      }
    }.flatMap { list =>
      Future.sequence(list)
    }
  }

  def cleanupOnFailure(): PartialFunction[Throwable, Future[Assertion]] = PartialFunction.fromFunction { exception =>
    resetUserDatabase()
      .map { _ =>
        throw exception
      }
  }

  def cleanupOnSuccess(assertion: Assertion): Future[Assertion] = {
    resetUserDatabase()
      .map { _ =>
        assertion
      }
  }

  private def retrieveTable(role: Role): Future[Seq[String]] = {
    server.application.database.getAll(role)
  }

  //Additional variables needed for some tests
  val student0UpdatedUneditable: Student = student0.copy(latestImmatriculation = "SS2012")
  val student0UpdatedProtected: Student = student0UpdatedUneditable.copy(firstName = "Dieter", lastName = "Dietrich", birthDate = "1996-12-11", matriculationId = "1333337")
  val uneditableErrorSize: Int = 1
  val protectedErrorSize: Int = 4 + uneditableErrorSize

  val lecturer0Updated: Lecturer = lecturer0.copy(email = "noreply@scam.ng", address = address1, freeText = "Morgen kommt der große Gauss.", researchArea = "Physics")

  /** Tests only working if the whole instance is started */
  "UserService service" should {

    "get all users with default users" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getAllUsers(None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>

          answer.admins should have size 1
          answer.lecturers should have size 1
          answer.students should have size 1
        }
      }
    }

    //ADD TESTS
    "add a student" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader()).invoke(PostMessageStudent(student0Auth, student0))
      eventually(timeout(Span(15, Seconds))) {
        client.getAllStudents(None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer should contain(student0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding a user with different username in authUser" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader())
        .invoke(PostMessageAdmin(admin0Auth.copy(username = admin0.username + "changed"), admin0))
        .failed.map {
          answer => answer.asInstanceOf[UC4Exception].errorCode.http should ===(422)
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding an already existing User" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.addUser().handleRequestHeader(addAuthorizationHeader())
          .invoke(PostMessageAdmin(admin0Auth, admin0.copy(firstName = "Dieter"))).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
              .invalidParams should contain(SimpleError("admin.username", "Username already in use."))
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding a User with an empty username" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.addUser().handleRequestHeader(addAuthorizationHeader())
          .invoke(PostMessageAdmin(admin0Auth, admin0.copy(firstName = "Dieter"))).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
              .invalidParams.map(_.name) should contain("admin.username")
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding a Student with a duplicate matriculationId" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.addUser().handleRequestHeader(addAuthorizationHeader())
          .invoke(PostMessageStudent(student0Auth.copy(username = "student7"), student0.copy(username = "student7"))).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse
              .asInstanceOf[DetailedError].invalidParams.map(_.name) should contain theSameElementsAs Seq("student.matriculationId")
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    //GET TESTS
    "fetch the information of a User as an Admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer should ===(student0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch the information of a User as the User (non-Admin) himself" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().flatMap { answer =>
          answer should ===(student0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch the public information of a User as another User (non-Admin)" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should ===(student0.toPublic)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch the public information of all specified Students, as a non-Admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getAllStudents(Some(student0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(student0.toPublic)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the public information of all specified Lecturers, as a non-Admin" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.getAllLecturers(Some(lecturer0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(lecturer0.toPublic)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the public information of all specified Admins, as a non-Admin" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.getAllAdmins(Some(admin0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(admin0.toPublic)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the information of all specified Students, as an Admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getAllStudents(Some(student0.username)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(student0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the information of all specified Lecturers, as an Admin" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.getAllLecturers(Some(lecturer0.username)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(lecturer0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the information of all specified Admins, as an Admin" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.getAllAdmins(Some(admin0.username)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(admin0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the public information of all specified Users, as a non-Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should ===(GetAllUsersResponse(Seq(student0.toPublic), Seq(lecturer0.toPublic), Seq(admin0.toPublic)))
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the information of all specified Users, as an Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer should ===(GetAllUsersResponse(Seq(student0), Seq(lecturer0), Seq(admin0)))
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "find a non-existing User" in {
      client.getUser("Guten Abend").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { answer =>
        answer.asInstanceOf[UC4Exception].errorCode.http should ===(404)
      }
    }

    "get the role of a user" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.getRole(lecturer0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer.role shouldBe Role.Lecturer
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    //UPDATE TESTS
    "not update a non-existing User" in {
      client.updateUser("GutenAbend").handleRequestHeader(addAuthorizationHeader())
        .invoke(student0.copy(username = "GutenAbend")).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
            .invalidParams.map(_.name) should contain("username")
        }
    }

    "update a user as an admin" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.updateUser(admin0.username).handleRequestHeader(addAuthorizationHeader())
          .invoke(admin0.copy(firstName = "KLAUS")).flatMap { _ =>
            client.getUser(admin0.username).handleRequestHeader(addAuthorizationHeader()).invoke()
          }.flatMap { answer =>
            answer.firstName shouldBe "KLAUS"
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "update a user as the user himself" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.updateUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username))
          .invoke(lecturer0Updated).flatMap { _ =>
            client.getUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader()).invoke()
          }.flatMap { answer =>
            answer should ===(lecturer0Updated)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update uneditable fields as an admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.updateUser(student0UpdatedUneditable.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedUneditable.username))
          .invoke(student0UpdatedUneditable).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
              have length uneditableErrorSize
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update protected fields as the user himself" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.updateUser(student0UpdatedProtected.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedProtected.username))
          .invoke(student0UpdatedProtected).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
              have length protectedErrorSize
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    //DELETE TESTS
    "delete a non-existing user" in {
      client.deleteUser("Guten Abend").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].errorCode.http should ===(404)
      }
    }
    "delete a user from the database" in {
      prepare(Seq(student0)).flatMap { createdUser =>
        client.deleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          eventually(timeout(Span(15, Seconds))) {
            retrieveTable(createdUser.head.role).map {
              usernames =>
                usernames should not contain createdUser.head.username
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    //IMAGE TESTS
    "should upload an image, which" must {
      "save the same image" in {
        prepare(Seq(student0)).flatMap { _ =>
          val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example.png"))
          val putHeader = FakeRequest(PUT, s"/user-management/users/${student0.username}/image",
            FakeHeaders(Seq(
              ("Content-Type", "image/png"),
              ("Cookie", createLoginToken(student0.username))
            )), body)

          val getHeader = FakeRequest(GET, s"/user-management/users/${student0.username}/image",
            FakeHeaders(Seq(
              ("Cookie", createLoginToken(student0.username))
            )), "null")

          route(server.application.application, putHeader).get.flatMap { _ =>

            eventually(timeout(Span(30, Seconds))) {
              val image = contentAsBytes(
                route(server.application.application, getHeader).get
              )(akka.util.Timeout(20.seconds)).toArray

              image should contain theSameElementsInOrderAs body
            }
          }
        }.flatMap(cleanupOnSuccess)
          .recoverWith(cleanupOnFailure())
      }

      "save the right content type" in {
        prepare(Seq(student0)).flatMap { _ =>
          val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example.png"))
          val putHeader = FakeRequest(PUT, s"/user-management/users/${student0.username}/image",
            FakeHeaders(Seq(
              ("Content-Type", "image/png"),
              ("Cookie", createLoginToken(student0.username))
            )), body)

          val getHeader = FakeRequest(GET, s"/user-management/users/${student0.username}/image",
            FakeHeaders(Seq(
              ("Cookie", createLoginToken(student0.username))
            )), "null")

          route(server.application.application, putHeader).get.flatMap { _ =>
            route(server.application.application, getHeader).get.map { result =>
              result.body.contentType.get should ===("image/png; charset=UTF-8")
            }
          }
        }.flatMap(cleanupOnSuccess)
          .recoverWith(cleanupOnFailure())
      }

      "return the right location header" in {
        prepare(Seq(student0)).flatMap { _ =>
          val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example.png"))
          val putHeader = FakeRequest(PUT, s"/user-management/users/${student0.username}/image",
            FakeHeaders(Seq(
              ("Content-Type", "image/png"),
              ("Cookie", createLoginToken(student0.username))
            )), body)

          route(server.application.application, putHeader).get.flatMap { putResult =>

            val getHeader = FakeRequest(GET, putResult.header.headers("location"),
              FakeHeaders(Seq(
                ("Cookie", createLoginToken(student0.username))
              )), "null")

            route(server.application.application, getHeader).get.map { getResult =>
              getResult.header.status should ===(200)
            }
          }
        }.flatMap(cleanupOnSuccess)
          .recoverWith(cleanupOnFailure())
      }
    }
    "should reject too large images" in {
      prepare(Seq(student0)).map { _ =>
        val body = Array.ofDim[Byte](server.application.config.getInt("uc4.image.maxSize") + 1)
        val putHeader = FakeRequest(PUT, s"/user-management/users/${student0.username}/image",
          FakeHeaders(Seq(
            ("Content-Type", "image/png"),
            ("Cookie", createLoginToken(student0.username))
          )), body)

        val json = contentAsJson(route(server.application.application, putHeader).get)(akka.util.Timeout(5.seconds))
        json.as[UC4Error].`type` should ===(ErrorType.EntityTooLarge)

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "should reject images with not supported media types" in {
      prepare(Seq(student0)).map { _ =>
        val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example.svg"))
        val putHeader = FakeRequest(PUT, s"/user-management/users/${student0.username}/image",
          FakeHeaders(Seq(
            ("Content-Type", "image/svg"),
            ("Cookie", createLoginToken(student0.username))
          )), body)

        val json = contentAsJson(route(server.application.application, putHeader).get)(akka.util.Timeout(5.seconds))
        json.as[UC4Error].`type` should ===(ErrorType.UnsupportedMediaType)

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "should return the default image if no picture is set" in {
      prepare(Seq(admin0)).map { _ =>
        val default = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultProfile.png"))
        val getHeader = FakeRequest(GET, s"/user-management/users/${admin0.username}/image",
          FakeHeaders(Seq(
            ("Cookie", createLoginToken(student0.username))
          )), "null")

        val image = contentAsBytes(
          route(server.application.application, getHeader).get
        )(akka.util.Timeout(20.seconds)).toArray

        image should contain theSameElementsInOrderAs default

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "should delete an image of a user" in {
      prepare(Seq(student0)).flatMap { _ =>
        val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example.png"))
        val putHeader = FakeRequest(PUT, s"/user-management/users/${student0.username}/image",
          FakeHeaders(Seq(
            ("Content-Type", "image/png"),
            ("Cookie", createLoginToken(student0.username))
          )), body)

        val deleteHeader = FakeRequest(DELETE, s"/user-management/users/${student0.username}/image",
          FakeHeaders(Seq(
            ("Cookie", createLoginToken(student0.username))
          )), "null")

        val default = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultProfile.png"))
        val getHeader = FakeRequest(GET, s"/user-management/users/${student0.username}/image",
          FakeHeaders(Seq(
            ("Cookie", createLoginToken(student0.username))
          )), "null")

        route(server.application.application, putHeader).get.flatMap { _ =>

          eventually(timeout(Span(30, Seconds))) {
            val image = contentAsBytes(
              route(server.application.application, getHeader).get
            )(akka.util.Timeout(20.seconds)).toArray

            image should contain theSameElementsInOrderAs body
          }.flatMap { _ =>
            route(server.application.application, deleteHeader).get

            eventually(timeout(Span(30, Seconds))) {
              val image = contentAsBytes(
                route(server.application.application, getHeader).get
              )(akka.util.Timeout(20.seconds)).toArray

              image should contain theSameElementsInOrderAs default
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
  }
}
