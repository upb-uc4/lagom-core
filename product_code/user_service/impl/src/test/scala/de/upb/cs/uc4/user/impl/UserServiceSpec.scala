package de.upb.cs.uc4.user.impl

import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString
import com.google.common.io.ByteStreams
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.authentication.AuthenticationServiceStub
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser, JsonUsername }
import de.upb.cs.uc4.image.ImageProcessingServiceStub
import de.upb.cs.uc4.image.api.ImageProcessingService
import de.upb.cs.uc4.shared.client.Hashing
import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.DefaultTestUsers
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model._
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll }
import play.api.test.Helpers.{ DELETE, GET, PUT, contentAsBytes, contentAsJson, route }
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.Future
import scala.concurrent.duration._

/** Tests for the UserService */
class UserServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with Eventually with DefaultTestUsers {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new UserApplication(ctx) with LocalServiceLocator with TestTopicComponents {

        override lazy val authentication: AuthenticationService = new AuthenticationServiceStub()

        override lazy val imageProcessing: ImageProcessingService = new ImageProcessingServiceStub()
      }
    }

  implicit val system: ActorSystem = server.actorSystem
  implicit val mat: Materializer = server.materializer

  val client: UserService = server.serviceClient.implement[UserService]

  val deletionTopic: Source[EncryptionContainer, _] = client.userDeletionTopicMinimal().subscribe.atMostOnceSource

  override protected def afterAll(): Unit = server.stop()

  def prepare(users: Seq[User]): Future[Seq[User]] = {
    Future.sequence(users.map { user =>
      val newUsername = user.username
      val postMessage = user match {
        case s: Student  => PostMessageUser(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Student), "governmentIdStudent", s.copy(username = newUsername))
        case l: Lecturer => PostMessageUser(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Lecturer), "governmentIdLecturer", l.copy(username = newUsername))
        case a: Admin    => PostMessageUser(AuthenticationUser(newUsername, newUsername, AuthenticationRole.Admin), "governmentIdAdmin", a.copy(username = newUsername))
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
        client.getAllStudents(None, None).handleRequestHeader(addAuthorizationHeader()).invoke()
      }
      else {
        Future.successful(Seq())
      }
      lecturerList <- if (users.exists(_.role == Role.Lecturer)) {
        client.getAllLecturers(None, None).handleRequestHeader(addAuthorizationHeader()).invoke()
      }
      else {
        Future.successful(Seq())
      }
      adminList <- if (users.exists(_.role == Role.Admin)) {
        client.getAllAdmins(None, None).handleRequestHeader(addAuthorizationHeader()).invoke()
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
          usernamesStudent ++ usernamesLecturer ++ usernamesAdmin should contain theSameElementsAs Seq("admin")
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
      case Role.Student  => client.getAllStudents(None, None).handleRequestHeader(addAuthorizationHeader()).invoke()
      case Role.Lecturer => client.getAllLecturers(None, None).handleRequestHeader(addAuthorizationHeader()).invoke()
      case Role.Admin    => client.getAllAdmins(None, None).handleRequestHeader(addAuthorizationHeader()).invoke()
    }
    futureUserList.map {
      _.filter(_.username != roleString).map { user =>
        client.forceDeleteUser(user.username).handleRequestHeader(addAuthorizationHeader()).invoke()
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

  private def createUsernames(username: String, governmentId: String, enrollmentIdSecret: String): Usernames = Usernames(username, Hashing.sha256(s"$governmentId$enrollmentIdSecret"))

  //Additional variables needed for some tests
  val student0UpdatedUneditable: Student = student0.copy(latestImmatriculation = "SS2012", enrollmentIdSecret = "newEnrollmentIdSecret", isActive = false)
  val student0UpdatedProtected: Student = student0UpdatedUneditable.copy(firstName = "Dieter", lastName = "Dietrich", birthDate = "1996-12-11", matriculationId = "1333337")
  val uneditableErrorSize: Int = 3
  val protectedErrorSize: Int = 4 + uneditableErrorSize

  val lecturer0Updated: Lecturer = lecturer0.copy(email = "noreply@scam.ng", address = address1, freeText = "Morgen kommt der große Gauss.", researchArea = "Physics")

  /** Tests only working if the whole instance is started */
  "UserService service" should {

    "test topics, which" must {
      "publish a created user" in {
        val creationSource: Source[EncryptionContainer, _] = client.userCreationTopic().subscribe.atMostOnceSource
        client.addUser().handleRequestHeader(addAuthorizationHeader()).invoke(PostMessageUser(student0Auth, "governmentIdStudent0", student0)).map {
          student0Created =>
            val source = creationSource
              .runWith(TestSink.probe[EncryptionContainer]).request(2)

            val containerSeq = Seq(
              source.expectNext(FiniteDuration(15, SECONDS)),
              source.expectNext(FiniteDuration(15, SECONDS))
            )

            containerSeq.map {
              container =>
                server.application.kafkaEncryptionUtility.decrypt[Usernames](container)
            } should contain theSameElementsAs Seq(
              createUsernames("admin", "governmentIdAdmin", "YWRtaW5hZG1pbg=="),
              createUsernames(student0.username, "governmentIdStudent0", student0Created.enrollmentIdSecret)
            )
        }

      }

      "publish a force-deleted user in minimal format" in {
        val deletionSource: Source[EncryptionContainer, _] = client.userDeletionTopicMinimal().subscribe.atMostOnceSource
        client.forceDeleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke()

        val container: EncryptionContainer = deletionSource
          .runWith(TestSink.probe[EncryptionContainer])
          .request(1)
          .expectNext(FiniteDuration(15, SECONDS))
        server.application.kafkaEncryptionUtility.decrypt[JsonUsername](container) should ===(JsonUsername(student0.username))
      }

      "publish a force-deleted user in precise format" in {
        val deletionSource: Source[EncryptionContainer, _] = client.userDeletionTopicPrecise().subscribe.atMostOnceSource
        client.forceDeleteUser(student1.username).handleRequestHeader(addAuthorizationHeader()).invoke()

        val container: EncryptionContainer = deletionSource
          .runWith(TestSink.probe[EncryptionContainer])
          .request(1)
          .expectNext(FiniteDuration(15, SECONDS))
        server.application.kafkaEncryptionUtility.decrypt[JsonUserData](container) should ===(JsonUserData(student0.username, Role.Student, forceDelete = true))
      }

      "publish a soft-deleted user in minimal format" in {
        val deletionSource: Source[EncryptionContainer, _] = client.userDeletionTopicMinimal().subscribe.atMostOnceSource
        client.softDeleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke()

        val container: EncryptionContainer = deletionSource
          .runWith(TestSink.probe[EncryptionContainer])
          .request(1)
          .expectNext(FiniteDuration(15, SECONDS))
        server.application.kafkaEncryptionUtility.decrypt[JsonUsername](container) should ===(JsonUsername(student0.username))
      }

      "publish a soft-deleted user in precise format" in {
        val deletionSource: Source[EncryptionContainer, _] = client.userDeletionTopicPrecise().subscribe.atMostOnceSource
        client.softDeleteUser(student1.username).handleRequestHeader(addAuthorizationHeader()).invoke()

        val container: EncryptionContainer = deletionSource
          .runWith(TestSink.probe[EncryptionContainer])
          .request(1)
          .expectNext(FiniteDuration(15, SECONDS))
        server.application.kafkaEncryptionUtility.decrypt[JsonUserData](container) should ===(JsonUserData(student0.username, Role.Student, forceDelete = true))
      }

    }

    "get all users with default users" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getAllUsers(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>

          answer.admins should have size 1
          answer.lecturers should have size 0
          answer.students should have size 0
        }
      }
    }

    //ADD TESTS
    "add a student" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader()).invoke(PostMessageUser(student0Auth, "governmentIdStudent", student0))
      eventually(timeout(Span(15, Seconds))) {
        client.getAllStudents(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer.map(_.copy(enrollmentIdSecret = "")) should contain(student0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding a user with different username in authUser" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader())
        .invoke(PostMessageUser(admin0Auth.copy(username = admin0.username + "changed"), "governmentIdAdmin", admin0))
        .failed.map {
          answer => answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding an already existing User" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.addUser().handleRequestHeader(addAuthorizationHeader())
          .invoke(PostMessageUser(admin0Auth, "governmentIdAdmin", admin0.copy(firstName = "Dieter"))).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
              .invalidParams should contain(SimpleError("user.username", "Username already in use."))
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding a User with an empty username" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.addUser().handleRequestHeader(addAuthorizationHeader())
          .invoke(PostMessageUser(admin0Auth, "governmentIdAdmin", admin0.copy(firstName = "Dieter"))).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
              .invalidParams.map(_.name) should contain("user.username")
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail on adding a Student with a duplicate matriculationId" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.addUser().handleRequestHeader(addAuthorizationHeader())
          .invoke(PostMessageUser(student0Auth.copy(username = "student7"), "governmentIdAdmin", student0.copy(username = "student7"))).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse
              .asInstanceOf[DetailedError].invalidParams.map(_.name) should contain theSameElementsAs Seq("user.matriculationId")
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    //GET TESTS
    "fetch the information of a User as an Admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer.copyUser(enrollmentIdSecret = "") should ===(student0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch the information of a User as the User (non-Admin) himself" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().flatMap { answer =>
          answer.copyUser(enrollmentIdSecret = "") should ===(student0)
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
        client.getAllStudents(Some(student0.username), None).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(student0.toPublic)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the public information of all specified Lecturers, as a non-Admin" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.getAllLecturers(Some(lecturer0.username), None).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(lecturer0.toPublic)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the public information of all specified Admins, as a non-Admin" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.getAllAdmins(Some(admin0.username), None).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should contain theSameElementsAs Seq(admin0.toPublic)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the information of all specified Students, as an Admin" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.getAllStudents(Some(student0.username), None).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer.map(_.copy(enrollmentIdSecret = "")) should contain theSameElementsAs Seq(student0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the information of all specified Lecturers, as an Admin" in {
      prepare(Seq(lecturer0)).flatMap { _ =>
        client.getAllLecturers(Some(lecturer0.username), None).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer.map(_.copy(enrollmentIdSecret = "")) should contain theSameElementsAs Seq(lecturer0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the information of all specified Admins, as an Admin" in {
      prepare(Seq(admin0)).flatMap { _ =>
        client.getAllAdmins(Some(admin0.username), None).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer.map(_.copy(enrollmentIdSecret = "")) should contain theSameElementsAs Seq(admin0)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch the public information of all specified Users, as a non-Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username), None).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
          answer should ===(GetAllUsersResponse(Seq(student0.toPublic), Seq(lecturer0.toPublic), Seq(admin0.toPublic)))
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch the public information of all specified Users that are active, as a non-Admin" in {
      prepare(Seq(student0, student1, student2, lecturer0, lecturer1, admin0, admin1)).flatMap { _ =>
        client.softDeleteUser(student1.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          client.softDeleteUser(lecturer1.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
            client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username + "," + student1.username + "," + lecturer1.username), Some(true)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
              answer should ===(GetAllUsersResponse(Seq(student0.toPublic), Seq(lecturer0.toPublic), Seq(admin0.toPublic)))
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch information of all specified Users that are inactive, as a non-Admin" in {
      prepare(Seq(student0, student1, student2, lecturer0, lecturer1, admin0, admin1)).flatMap { _ =>
        client.softDeleteUser(student1.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          client.softDeleteUser(lecturer1.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
            client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username + "," + student1.username + "," + lecturer1.username), Some(false)).handleRequestHeader(addAuthorizationHeader("student")).invoke().flatMap { answer =>
              answer should ===(GetAllUsersResponse(Seq(student1.softDelete), Seq(lecturer1.softDelete), Seq()))
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch the information of all specified Users, as an Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username), None).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
          answer.copy(
            answer.students.map(_.copy(enrollmentIdSecret = "")),
            answer.lecturers.map(_.copy(enrollmentIdSecret = "")),
            answer.admins.map(_.copy(enrollmentIdSecret = ""))
          ) should ===(GetAllUsersResponse(Seq(student0), Seq(lecturer0), Seq(admin0)))
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch all active Users from the database as an Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { addedUsers =>
        client.softDeleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          eventually(timeout(Span(15, Seconds))) {
            client.getAllUsers(None, Some(true)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { allUsers =>

              allUsers.students should have size 0
              allUsers.lecturers should contain allElementsOf addedUsers.filter(_.role == Role.Lecturer)
              allUsers.admins should have size 2 //Has size two because system admin is always added
              allUsers.admins should contain allElementsOf addedUsers.filter(_.role == Role.Admin)
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch all active Students from the database as an Admin" in {
      prepare(Seq(student0, student1, student2)).flatMap { addedUsers: Seq[User] =>
        client.softDeleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          eventually(timeout(Span(15, Seconds))) {
            client.getAllUsers(None, Some(true)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { allUsers =>

              allUsers.students should contain theSameElementsAs addedUsers.filter(user => user.role == Role.Student && user.username != student0.username)
              allUsers.lecturers should have size 0
              allUsers.admins should have size 1
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch all non-active Students from the database as an Admin" in {
      prepare(Seq(student0, student1, student2)).flatMap { addedUsers: Seq[User] =>
        client.softDeleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          eventually(timeout(Span(15, Seconds))) {
            client.getAllUsers(None, Some(false)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { allUsers =>

              allUsers.students.map(_.username) should contain theSameElementsAs addedUsers.filter(_.username == student0.username).map(_.username)
              allUsers.lecturers should have size 0
              allUsers.admins should have size 0
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch all active Lecturers from the database as an Admin" in {
      prepare(Seq(lecturer0, lecturer1, lecturer2)).flatMap { addedUsers: Seq[User] =>
        client.softDeleteUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          eventually(timeout(Span(15, Seconds))) {
            client.getAllUsers(None, Some(true)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { allUsers =>

              allUsers.students should have size 0
              allUsers.lecturers should contain theSameElementsAs addedUsers.filter(user => user.role == Role.Lecturer && user.username != lecturer0.username)
              allUsers.admins should have size 1
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch all non-active Lecturers from the database as an Admin" in {
      prepare(Seq(lecturer0, lecturer1, lecturer2)).flatMap { addedUsers: Seq[User] =>
        client.softDeleteUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          eventually(timeout(Span(15, Seconds))) {
            client.getAllUsers(None, Some(false)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { allUsers =>

              allUsers.students.map(_.username) should have size 0
              allUsers.lecturers.map(_.username) should contain theSameElementsAs addedUsers.filter(_.username == lecturer0.username).map(_.username)
              allUsers.admins should have size 0
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch all active Admins from the database as an Admin" in {
      prepare(Seq(admin0, admin1, admin2)).flatMap { addedUsers: Seq[User] =>
        val userToDelete = addedUsers.head
        client.softDeleteUser(userToDelete.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          // val currentlyActive = addedUsers.diff()
          eventually(timeout(Span(15, Seconds))) {
            client.getAllUsers(None, Some(true)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { allUsers =>
              allUsers.students should have size 0
              allUsers.lecturers should have size 0
              allUsers.admins should have size 3 //Has size three because system admin is always added
              allUsers.admins should contain allElementsOf addedUsers.filter(_.role == Role.Admin).diff(Seq(userToDelete)) //without userToDelete

            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch all non-active Admins from the database as an Admin" in {
      prepare(Seq(admin0, admin1, admin2)).flatMap { addedUsers: Seq[User] =>
        client.softDeleteUser(admin0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          eventually(timeout(Span(15, Seconds))) {
            client.getAllUsers(None, Some(false)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { allUsers =>

              allUsers.students.map(_.username) should have size 0
              allUsers.lecturers should have size 0
              allUsers.admins.map(_.username) should contain theSameElementsAs addedUsers.filter(_.username == admin0.username).map(_.username)
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fetch only the information of all specified Users that are active, as an Admin" in {
      prepare(Seq(student0, student1, student2, lecturer0, lecturer1, admin0, admin1)).flatMap { _ =>
        client.softDeleteUser(student1.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          client.softDeleteUser(lecturer1.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
            client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username + "," + student1.username + "," + lecturer1.username), Some(true)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
              answer.copy(
                answer.students.map(_.copy(enrollmentIdSecret = "")),
                answer.lecturers.map(_.copy(enrollmentIdSecret = "")),
                answer.admins.map(_.copy(enrollmentIdSecret = ""))
              ) should ===(GetAllUsersResponse(Seq(student0), Seq(lecturer0), Seq(admin0)))
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "fetch only the information of all specified Users that are inactive, as an Admin" in {
      prepare(Seq(student0, student1, student2, lecturer0, lecturer1, admin0, admin1)).flatMap { _ =>
        client.softDeleteUser(student1.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          client.softDeleteUser(lecturer1.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
            client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username + "," + student1.username + "," + lecturer1.username), Some(false)).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
              answer.copy(
                answer.students.map(_.copy(enrollmentIdSecret = "")),
                answer.lecturers.map(_.copy(enrollmentIdSecret = "")),
                answer.admins.map(_.copy(enrollmentIdSecret = ""))
              ) should ===(GetAllUsersResponse(Seq(student1.softDelete), Seq(lecturer1.softDelete), Seq()))
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not fetch the public information of all Users, as an non-Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllUsers(None, None).handleRequestHeader(addAuthorizationHeader("student")).invoke().failed.flatMap { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.NotEnoughPrivileges)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not fetch the public information of all Students, as an non-Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllStudents(None, None).handleRequestHeader(addAuthorizationHeader("student")).invoke().failed.flatMap { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.NotEnoughPrivileges)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "not fetch the public information of all Lecturers, as an non-Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllLecturers(None, None).handleRequestHeader(addAuthorizationHeader("student")).invoke().failed.flatMap { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.NotEnoughPrivileges)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "not fetch the public information of all Admins, as an non-Admin" in {
      prepare(Seq(student0, lecturer0, admin0)).flatMap { _ =>
        client.getAllAdmins(None, None).handleRequestHeader(addAuthorizationHeader("student")).invoke().failed.flatMap { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.NotEnoughPrivileges)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "not find a non-existing User" in {
      client.getUser("Guten Abend").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { answer =>
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
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
      prepare(Seq(admin0)).flatMap { userList =>
        val enrollmentIdSecretFetched = userList.find(_.username == admin0.username).get.enrollmentIdSecret
        val admin0FetchedAndUpdated = admin0.copy(firstName = "KLAUS", enrollmentIdSecret = enrollmentIdSecretFetched)
        client.updateUser(admin0.username).handleRequestHeader(addAuthorizationHeader())
          .invoke(admin0FetchedAndUpdated).flatMap { _ =>
            client.getUser(admin0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { answer =>
              answer.firstName shouldBe "KLAUS"
            }
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "update a user as the user himself" in {
      prepare(Seq(lecturer0)).flatMap { userList =>
        val enrollmentIdSecretFetched = userList.find(_.username == lecturer0.username).get.enrollmentIdSecret
        val lecturer0FetchedAndUpdated = lecturer0Updated.copy(enrollmentIdSecret = enrollmentIdSecretFetched)
        client.updateUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username))
          .invoke(lecturer0FetchedAndUpdated).flatMap { _ =>
            client.getUser(lecturer0FetchedAndUpdated.username).handleRequestHeader(addAuthorizationHeader()).invoke()
          }.flatMap { answer =>
            answer should ===(lecturer0FetchedAndUpdated)
          }

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update a user with another username in path" in {
      prepare(Seq(lecturer0)).flatMap { userList =>
        val enrollmentIdSecretFetched = userList.find(_.username == lecturer0.username).get.enrollmentIdSecret
        val lecturer0FetchedAndUpdated = lecturer0Updated.copy(enrollmentIdSecret = enrollmentIdSecretFetched)

        client.updateUser(lecturer0.username + "thisShouldFail").handleRequestHeader(addAuthorizationHeader(lecturer0.username))
          .invoke(lecturer0FetchedAndUpdated).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.PathParameterMismatch)
          }

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update a user with a malformed username" in {
      prepare(Seq(lecturer0)).flatMap { userList =>
        val enrollmentIdSecretFetched = userList.find(_.username == lecturer0.username).get.enrollmentIdSecret
        val lecturer0FetchedAndUpdated = lecturer0Updated.copy(username = lecturer0.username + ":)", enrollmentIdSecret = enrollmentIdSecretFetched)

        client.updateUser(lecturer0.username + ":)").handleRequestHeader(addAuthorizationHeader(lecturer0.username + ":)"))
          .invoke(lecturer0FetchedAndUpdated).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
              .invalidParams should contain(SimpleError("username", ErrorMessageCollection.User.usernameMessage))
          }

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update a user with a malformed new email" in {
      prepare(Seq(lecturer0)).flatMap { userList =>
        val enrollmentIdSecretFetched = userList.find(_.username == lecturer0.username).get.enrollmentIdSecret
        val lecturer0FetchedAndUpdated = lecturer0Updated.copy(enrollmentIdSecret = enrollmentIdSecretFetched, email = "@@")

        client.updateUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username))
          .invoke(lecturer0FetchedAndUpdated).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
              .invalidParams should contain(SimpleError("email", ErrorMessageCollection.User.mailMessage))
          }

      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update another user as the user himself (as a non admin)" in {
      prepare(Seq(lecturer0)).flatMap { userList =>
        val enrollmentIdSecretFetched = userList.find(_.username == lecturer0.username).get.enrollmentIdSecret
        val lecturer0FetchedAndUpdated = lecturer0Updated.copy(enrollmentIdSecret = enrollmentIdSecretFetched)
        client.updateUser(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username + "weAreAnotherUserNow"))
          .invoke(lecturer0FetchedAndUpdated).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
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

    "update latest matriculation of a student" in {
      prepare(Seq(student0)).flatMap { _ =>
        client.updateLatestMatriculation().invoke(MatriculationUpdate(student0.username, "SS2020")).flatMap { _ =>
          client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader(admin0.username)).invoke().map {
            user =>
              user.asInstanceOf[Student].latestImmatriculation should ===("SS2020")
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    //DELETE TESTS
    "return an error on force deleting a non-existing user" in {
      client.forceDeleteUser("Guten Abend").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
      }
    }
    "force delete a user from the database" in {
      prepare(Seq(student0)).flatMap { createdUser =>
        client.forceDeleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
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

    "return an error on soft deleting a non-existing user" in {
      client.softDeleteUser("Guten Abend").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].errorCode should ===(404)
      }
    }
    "return an error on soft deleting an already soft deleted user" in {
      prepare(Seq(student0)).flatMap { createdUsers =>
        client.softDeleteUser(createdUsers.head.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap {
          _ =>
            client.softDeleteUser(createdUsers.head.username).handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
              exception =>
                exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.AlreadyDeleted)
            }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }
    "soft delete a user from the database" in {
      prepare(Seq(student0)).flatMap { createdUser =>
        client.softDeleteUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>
          eventually(timeout(Span(15, Seconds))) {
            client.getUser(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { deletedUser =>
              deletedUser should ===(createdUser.head.softDelete)
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
        val default = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultProfile.jpg"))
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

        val default = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultProfile.jpg"))
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

    "should get thumbnail of a user" in {
      val profilePicture = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example.png"))
      val thumbnail = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example_thumbnail.jpeg"))

      prepare(Seq(admin0)).flatMap { _ =>
        server.application.database.setImage(admin0.username, profilePicture, thumbnail).flatMap {
          _ =>
            client.getThumbnail(admin0.username).handleRequestHeader(addAuthorizationHeader(admin0.username)).invoke().map {
              thumb: ByteString =>
                thumb.toArray should contain theSameElementsInOrderAs thumbnail
            }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "should not set the image thumbnail of aother user" in {
      val profilePicture = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example.png"))
      val thumbnail = ByteStreams.toByteArray(getClass.getResourceAsStream("/Example_thumbnail.jpeg"))

      prepare(Seq(student1, student2)).flatMap { _ =>
        client.setImage(student1.username).handleRequestHeader(addAuthorizationHeader(student2.username)).invoke(profilePicture).failed.flatMap {
          answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "should get the default thumbnail of a user which has not set a thumbnail yet" in {
      val thumbnail = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultThumbnail.jpg"))
      prepare(Seq(admin0)).flatMap { _ =>
        client.getThumbnail(admin0.username).handleRequestHeader(addAuthorizationHeader(admin0.username)).invoke().map {
          thumb: ByteString =>
            thumb.toArray should contain theSameElementsInOrderAs thumbnail
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

  }
}
