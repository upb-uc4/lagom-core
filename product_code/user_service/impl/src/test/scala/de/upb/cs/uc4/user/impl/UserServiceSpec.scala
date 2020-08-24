package de.upb.cs.uc4.user.impl

import java.util.Base64

import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.test.AuthenticationServiceStub
import de.upb.cs.uc4.shared.client.exceptions.{ CustomException, DetailedError }
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent }
import de.upb.cs.uc4.user.model.user.{ Lecturer, Student }
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, JsonUsername, Role }
import de.upb.cs.uc4.user.test.DefaultTestUsers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Span }
import org.scalatest.wordspec.AsyncWordSpec

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class UserServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually with DefaultTestUsers {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new UserApplication(ctx) with LocalServiceLocator with TestTopicComponents {
        override lazy val authenticationService: AuthenticationService = new AuthenticationServiceStub()
      }
    }

  val client: UserService = server.serviceClient.implement[UserService]
  val deletionTopic: Source[JsonUsername, _] = client.userDeletedTopic().subscribe.atMostOnceSource

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(username: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString(s"$username:$username".getBytes()))
  }

  //Additional variables needed for some tests
  val student0UpdatedUneditable: Student = student0.copy(role = Role.Lecturer, latestImmatriculation = "SS2012")
  val student0UpdatedProtected: Student = student0UpdatedUneditable.copy(firstName = "Dieter", lastName = "Dietrich", birthDate = "1996-12-11", matriculationId = "1333337")
  val uneditableErrorSize: Int = 2
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

    "add a student" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin")).invoke(PostMessageStudent(student0Auth, student0))
      eventually(timeout(Span(2, Minutes))) {
        client.getAllStudents(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
          answer should contain(student0)
        }
      }
    }

    "add a lecturer" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin")).invoke(PostMessageLecturer(lecturer0Auth, lecturer0))
      eventually(timeout(Span(2, Minutes))) {
        client.getAllLecturers(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
          answer should contain(lecturer0)
        }
      }
    }

    "add an admin" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin")).invoke(PostMessageAdmin(admin0Auth, admin0))
      eventually(timeout(Span(2, Minutes))) {
        client.getAllAdmins(None).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
          answer should contain(admin0)
        }
      }
    }

    "fetch the information of a Student as an Admin" in {
      client.getStudent(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer should ===(student0)
      }
    }
    "fetch the information of a Lecturer as an Admin" in {
      client.getLecturer(lecturer0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer should ===(lecturer0)
      }
    }
    "fetch the information of an Admin as an Admin" in {
      client.getAdmin(admin0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer should ===(admin0)
      }
    }

    "fetch the information of a Student as the student himself" in {
      client.getStudent(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map { answer =>
        answer should ===(student0)
      }
    }
    "fetch the information of a Lecturer as the lecturer himself" in {
      client.getLecturer(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().map { answer =>
        answer should ===(lecturer0)
      }
    }

    "fetch the public information of a Student as another student" in {
      client.getStudent(student0.username).handleRequestHeader(addAuthorizationHeader("student")).invoke().map { answer =>
        answer should ===(student0.toPublic)
      }
    }
    "fetch the public information of a Lecturer as a student" in {
      client.getLecturer(lecturer0.username).handleRequestHeader(addAuthorizationHeader("student")).invoke().map { answer =>
        answer should ===(lecturer0.toPublic)
      }
    }
    "fetch the public information of an Admin as a lecturer" in {
      client.getAdmin(admin0.username).handleRequestHeader(addAuthorizationHeader("lecturer")).invoke().map { answer =>
        answer should ===(admin0.toPublic)
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
    "fetch the information of all specified Users, as a non-Admin" in {
      client.getAllUsers(Some(student0.username + "," + lecturer0.username + "," + admin0.username)).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer should ===(GetAllUsersResponse(Seq(student0), Seq(lecturer0), Seq(admin0)))
      }
    }

    "throw an exception on adding a user with different username in authUser" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(PostMessageAdmin(admin0Auth.copy(username = admin0.username + "changed"), admin0))
        .failed.map {
          answer => answer.asInstanceOf[CustomException].getErrorCode.http should ===(422)
        }
    }

    "delete a non-existing user" in {
      client.deleteUser("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed.map {
        answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "find a non-existing student" in {
      client.getStudent("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "find a non-existing lecturer" in {
      client.getLecturer("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "find a non-existing admin" in {
      client.getAdmin("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "update a non-existing student" in {
      client.updateStudent("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(student0.copy(username = "Guten Abend")).failed.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
        }
    }

    "update a non-existing lecturer" in {
      client.updateLecturer("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(lecturer0.copy(username = "Guten Abend")).failed.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
        }
    }

    "update a non-existing admin" in {
      client.updateAdmin("Guten Abend").handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(admin0.copy(username = "Guten Abend")).failed.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
        }
    }

    "add an already existing user" in {
      client.addUser().handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(PostMessageAdmin(admin0Auth, admin0.copy(firstName = "Dieter"))).failed.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(409)
        }
    }

    "update a user as an admin" in {
      client.updateAdmin(admin0.username).handleRequestHeader(addAuthorizationHeader("admin"))
        .invoke(admin0.copy(firstName = "KLAUS")).flatMap { _ =>
          client.getAdmin(admin0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
        }.map { answer =>
          answer.firstName shouldBe "KLAUS"
        }
    }

    "get a role of a user" in {
      client.getRole(lecturer0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().map { answer =>
        answer.role shouldBe Role.Lecturer
      }
    }

    "update a user as the user himself" in {
      client.updateLecturer(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username))
        .invoke(lecturer0Updated).flatMap { _ =>
          client.getLecturer(lecturer0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke()
        }.map { answer =>
          answer should ===(lecturer0Updated)
        }
    }

    "not update uneditable fields as an admin" in {
      client.updateStudent(student0UpdatedUneditable.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedUneditable.username))
        .invoke(student0UpdatedUneditable).failed.map { answer =>
          answer.asInstanceOf[CustomException].getPossibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
            have length uneditableErrorSize
        }
    }

    "not update protected fields as the user himself" in {
      client.updateStudent(student0UpdatedProtected.username).handleRequestHeader(addAuthorizationHeader(student0UpdatedProtected.username))
        .invoke(student0UpdatedProtected).failed.map { answer =>
          answer.asInstanceOf[CustomException].getPossibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
            have length protectedErrorSize
        }
    }

    "delete a user" in {
      client.deleteUser(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().flatMap { _ =>
        client.getStudent(student0.username).handleRequestHeader(addAuthorizationHeader("admin")).invoke().failed
      }.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

  }
}
