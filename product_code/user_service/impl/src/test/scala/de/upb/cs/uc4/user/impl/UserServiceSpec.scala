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
import de.upb.cs.uc4.shared.client.exceptions.{ CustomError, CustomException, DetailedError, ErrorType }
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
import org.scalatest.{ Assertion, BeforeAndAfterAll, BeforeAndAfterEach }
import play.api.test.Helpers.{ GET, PUT, contentAsBytes, contentAsJson, route }
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.Future
import scala.concurrent.duration._

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class UserServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually with DefaultTestUsers with BeforeAndAfterEach {

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
      eventually(timeout(Span(30, Seconds))) {
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

  val picture = "ÿØÿà \u0010JFIF \u0001\u0001\u0001 ` `  ÿá :Exif  MM *   \b \u0003Q\u0010 \u0001   \u0001\u0001   Q\u0011 \u0004   \u0001    Q\u0012 \u0004   \u0001        ÿÛ C \u0002\u0001\u0001\u0002\u0001\u0001\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0002\u0003\u0005\u0003\u0003\u0003\u0003\u0003\u0006\u0004\u0004\u0003\u0005\u0007\u0006\u0007\u0007\u0007\u0006\u0007\u0007\b\t\u000B\t\b\b\n\b\u0007\u0007\n\n\n\u000B\f\f\f\f\u0007\t\u000E\u000F\n\f\u000E\u000B\f\f\fÿÛ C\u0001\u0002\u0002\u0002\u0003\u0003\u0003\u0006\u0003\u0003\u0006\f\b\u0007\b\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\f\fÿÀ \u0011\b \u0005 \u0006\u0003\u0001\" \u0002\u0011\u0001\u0003\u0011\u0001ÿÄ \u001F  \u0001\u0005\u0001\u0001\u0001\u0001\u0001\u0001        \u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000BÿÄ µ\u0010 \u0002\u0001\u0003\u0003\u0002\u0004\u0003\u0005\u0005\u0004\u0004  \u0001}\u0001\u0002\u0003 \u0004\u0011\u0005\u0012!1A\u0006\u0013Qa\u0007\"q\u00142\u0081‘¡\b#B±Á\u0015RÑð$3br‚\t\n\u0016\u0017\u0018\u0019\u001A%&'()*456789:CDEFGHIJSTUVWXYZcdefghijstuvwxyzƒ„…†‡ˆ‰Š’“”•–—˜™š¢£¤¥¦§¨©ª²³´µ¶·¸¹ºÂÃÄÅÆÇÈÉÊÒÓÔÕÖ×ØÙÚáâãäåæçèéêñòóôõö÷øùúÿÄ \u001F\u0001 \u0003\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001      \u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000BÿÄ µ\u0011 \u0002\u0001\u0002\u0004\u0004\u0003\u0004\u0007\u0005\u0004\u0004 \u0001\u0002w \u0001\u0002\u0003\u0011\u0004\u0005!1\u0006\u0012AQ\u0007aq\u0013\"2\u0081\b\u0014B‘¡±Á\t#3Rð\u0015brÑ\n\u0016$4á%ñ\u0017\u0018\u0019\u001A&'()*56789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz‚ƒ„…†‡ˆ‰Š’“”•–—˜™š¢£¤¥¦§¨©ª²³´µ¶·¸¹ºÂÃÄÅÆÇÈÉÊÒÓÔÕÖ×ØÙÚâãäåæçèéêòóôõö÷øùúÿÚ \f\u0003\u0001 \u0002\u0011\u0003\u0011 ? ñßø\"7À\u000F\u0003xÇNÔ/|Yàÿ \nøÊçÄQÞ˜\u007F¶ôè¯cÒÒÅì—\u0011$ŠËºS~K7P @:¶J(¯ÏxÊ¤ãŒ‡+jð[6ºÉmòùï¹×—Öœc$»þˆÿÙ"

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
          answer => answer.asInstanceOf[CustomException].getErrorCode.http should ===(422)
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding an already existing User" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.addUser().handleRequestHeader(addAuthorizationHeader())
          .invoke(PostMessageAdmin(admin0Auth, admin0.copy(firstName = "Dieter"))).failed.flatMap { answer =>
            answer.asInstanceOf[CustomException].getErrorCode.http should ===(409)
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
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
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
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
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
            answer.asInstanceOf[CustomException].getPossibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
              have length uneditableErrorSize
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update protected fields as the user himself" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.updateUser(student0UpdatedProtected.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedProtected.username))
          .invoke(student0UpdatedProtected).failed.flatMap { answer =>
            answer.asInstanceOf[CustomException].getPossibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
              have length protectedErrorSize
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    //DELETE TESTS
    "delete a non-existing user" in {
      client.deleteUser("Guten Abend").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
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
          val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Ben.png"))
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
          val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Ben.png"))
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
          val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Ben.png"))
          val putHeader = FakeRequest(PUT, s"/user-management/users/${student0.username}/image",
            FakeHeaders(Seq(
              ("Content-Type", "image/png"),
              ("Cookie", createLoginToken(student0.username))
            )), body)

          route(server.application.application, putHeader).get.flatMap { postResult =>

            val getHeader = FakeRequest(GET, postResult.header.headers("location"),
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
        json.as[CustomError].`type` should ===(ErrorType.EntityTooLarge)

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "should reject images with not supported media types" in {
      prepare(Seq(student0)).map { _ =>
        val body = ByteStreams.toByteArray(getClass.getResourceAsStream("/Ben.svg"))
        val putHeader = FakeRequest(PUT, s"/user-management/users/${student0.username}/image",
          FakeHeaders(Seq(
            ("Content-Type", "image/svg"),
            ("Cookie", createLoginToken(student0.username))
          )), body)

        val json = contentAsJson(route(server.application.application, putHeader).get)(akka.util.Timeout(5.seconds))
        json.as[CustomError].`type` should ===(ErrorType.UnsupportedMediaType)

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
  }
}
