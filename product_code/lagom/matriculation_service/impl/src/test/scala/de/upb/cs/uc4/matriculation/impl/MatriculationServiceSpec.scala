package de.upb.cs.uc4.matriculation.impl

import java.util.Base64

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{RequestHeader, TransportErrorCode}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.authentication.model.{AuthenticationRole, AuthenticationUser}
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.{ImmatriculationData, PutMessageMatriculationData, SubjectMatriculation}
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, GenericError}
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model._
import de.upb.cs.uc4.user.model.post.{PostMessageAdmin, PostMessageLecturer, PostMessageStudent}
import de.upb.cs.uc4.user.model.user.{Admin, Lecturer, Student}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.Json

import scala.concurrent.Future

/** Tests for the MatriculationService
 *
 * All tests need to be started in the defined order
 */
class MatriculationServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val address: Address = Address("Gänseweg", "42a", "13337", "Entenhausen", "Germany")
  private val student0: Student = Student("dieter", Role.Student, address, "Dieter", "Dietrich", "Picture", "example@mail.de", "1990-12-11", "000001")
  private val student1: Student = Student("hans", Role.Student, address, "Hans", "Hansen", "Picture", "example@mail.de", "1990-12-11", "000002")
  private val student2: Student = Student("max", Role.Student, address, "Max", "Mustermann", "Picture", "example@mail.de", "1990-12-11", "000003")

  private val students: Seq[Student] = Seq(student0, student1, student2)

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
  ) { ctx =>
    new MatriculationApplication(ctx) with LocalServiceLocator {
      override lazy val authenticationService: AuthenticationService = new AuthenticationService {
        /** Checks if the username and password pair exists */
        override def check(user: String, pw: String): ServiceCall[NotUsed, (String, AuthenticationRole)] =
          ServiceCall { _ => Future.successful("admin", AuthenticationRole.Admin) }

        /** Sets the authentication data of a user */
        override def setAuthentication(): ServiceCall[AuthenticationUser, Done] =
          ServiceCall { _ => Future.successful(Done) }

        /** Changes the password of the given user */
        override def changePassword(username: String): ServiceCall[AuthenticationUser, Done] =
          ServiceCall { _ => Future.successful(Done) }

        /** Allows PUT */
        override def allowedPut: ServiceCall[NotUsed, Done] =
          ServiceCall { _ => Future.successful(Done) }

        /** This Methods needs to allow a GET-Method */
        override def allowVersionNumber: ServiceCall[NotUsed, Done] =
          ServiceCall { _ => Future.successful(Done) }
      }

      override lazy val userService: UserService = new UserService {
        override def getAllUsers: ServiceCall[NotUsed, GetAllUsersResponse] = ServiceCall { _ => Future.successful(null) }

        override def deleteUser(username: String): ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

        override def getAllStudents: ServiceCall[NotUsed, Seq[Student]] = ServiceCall { _ => Future.successful(null) }

        override def addStudent(): ServiceCall[PostMessageStudent, Student] = ServiceCall { _ => Future.successful(null) }

        override def getStudent(username: String): ServiceCall[NotUsed, Student] = ServiceCall { _ =>
          val optStudent = students.find(_.username == username)

          if (optStudent.isDefined) {
            Future.successful(optStudent.get)
          } else {
            throw new CustomException(TransportErrorCode(404, 1003, "Error"), GenericError("key not found"))
          }
        }

        override def updateStudent(username: String): ServiceCall[Student, Done] = ServiceCall { _ => Future.successful(Done) }

        override def getAllLecturers: ServiceCall[NotUsed, Seq[Lecturer]] = ServiceCall { _ => Future.successful(null) }

        override def addLecturer(): ServiceCall[PostMessageLecturer, Lecturer] = ServiceCall { _ => Future.successful(null) }

        override def getLecturer(username: String): ServiceCall[NotUsed, Lecturer] = ServiceCall { _ => Future.successful(null) }

        override def updateLecturer(username: String): ServiceCall[Lecturer, Done] = ServiceCall { _ => Future.successful(Done) }

        override def getAllAdmins: ServiceCall[NotUsed, Seq[Admin]] = ServiceCall { _ => Future.successful(null) }

        override def addAdmin(): ServiceCall[PostMessageAdmin, Admin] = ServiceCall { _ => Future.successful(null) }

        override def getAdmin(username: String): ServiceCall[NotUsed, Admin] = ServiceCall { _ => Future.successful(null) }

        override def updateAdmin(username: String): ServiceCall[Admin, Done] = ServiceCall { _ => Future.successful(null) }

        override def getRole(username: String): ServiceCall[NotUsed, JsonRole] = ServiceCall { _ => Future.successful(null) }

        override def allowedGetPut: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(null) }

        override def allowedGetPost: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

        override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

        override def allowedDelete: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

        override def userDeletedTopic(): Topic[JsonUsername] = null

        override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }
      }

      override lazy val hyperLedgerService: HyperLedgerService = new HyperLedgerService {
        private var jsonStringList: List[String] = List()

        override def write(transactionId: String): ServiceCall[Seq[String], Done] = ServiceCall { seq =>
          transactionId match {
            case "addMatriculationData" =>
              jsonStringList ++= seq
              Future.successful(Done)
            case "addEntryToMatriculationData" =>
              val matriculationID = seq.head
              val fieldOfStudy = seq.apply(1)
              val semester = seq.apply(2)
              var data = Json.parse(jsonStringList.find(json => json.contains(matriculationID)).get).as[ImmatriculationData]

              val optSubject = data.matriculationStatus.find(_.fieldOfStudy == fieldOfStudy)

              if (optSubject.isDefined) {
                data = data.copy(matriculationStatus = data.matriculationStatus.map { subject =>
                  if (subject != optSubject.get) {
                    subject
                  } else {
                    subject.copy(semesters = subject.semesters :+ semester)
                  }
                })
              } else {
                data = data.copy(matriculationStatus = data.matriculationStatus :+ SubjectMatriculation(fieldOfStudy, Seq(semester)))
              }

              jsonStringList = jsonStringList.filter(json => !json.contains(matriculationID)) :+ Json.stringify(Json.toJson(data))
              Future.successful(Done)
            case _ => Future.successful(Done)
          }
        }

        override def read(transactionId: String): ServiceCall[Seq[String], String] = ServiceCall { seq =>
          transactionId match {
            case "getMatriculationData" =>
              val mat = jsonStringList.find(json => json.contains(seq.head))
              if (mat.isDefined) {
                Future.successful(mat.get)
              } else {
                Future.failed(new CustomException(TransportErrorCode(404, 1003, "Error"), GenericError("key not found")))
              }
            case _ => Future.successful("")
          }
        }

        override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }
      }
    }
  }

  val client: MatriculationService = server.serviceClient.implement[MatriculationService]

  override protected def afterAll(): Unit = server.stop()

  def addAuthenticationHeader(): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString("MOCK:MOCK".getBytes()))
  }

  /** Tests only working if the whole instance is started */
  "MatriculationService service" should {

    "add matriculation data for a student" in {
      client.addMatriculationData(student0.username).handleRequestHeader(addAuthenticationHeader())
        .invoke(PutMessageMatriculationData("Computer Science", "SS2020")).flatMap { _ =>
        client.getMatriculationData(student0.username).handleRequestHeader(addAuthenticationHeader()).invoke().map {
          answer =>
            answer.matriculationId should ===(student0.matriculationId)
            answer.matriculationStatus should contain theSameElementsAs
              Seq(SubjectMatriculation("Computer Science", Seq("SS2020")))
        }
      }
    }

    "extend matriculation data of an already existing field of study" in {
      client.addMatriculationData(student0.username).handleRequestHeader(addAuthenticationHeader())
        .invoke(PutMessageMatriculationData("Computer Science", "WS2020/21")).flatMap { _ =>
        client.getMatriculationData(student0.username).handleRequestHeader(addAuthenticationHeader()).invoke().map {
          answer =>
            answer.matriculationId should ===(student0.matriculationId)
            answer.matriculationStatus should contain theSameElementsAs
              Seq(SubjectMatriculation("Computer Science", Seq("SS2020", "WS2020/21")))
        }
      }
    }

    "extend matriculation data of a non-existing field of study" in {
      client.addMatriculationData(student0.username).handleRequestHeader(addAuthenticationHeader())
        .invoke(PutMessageMatriculationData("Mathematics", "WS2020/21")).flatMap { _ =>
        client.getMatriculationData(student0.username).handleRequestHeader(addAuthenticationHeader()).invoke().map {
          answer =>
            answer.matriculationId should ===(student0.matriculationId)
            answer.matriculationStatus should contain theSameElementsAs
              Seq(
                SubjectMatriculation("Computer Science", Seq("SS2020", "WS2020/21")),
                SubjectMatriculation("Mathematics", Seq("WS2020/21"))
              )
        }
      }
    }
  }
}
