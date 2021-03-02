package de.upb.cs.uc4.examresult.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.course.CourseServiceStub
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.exam.DefaultTestExams
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.exam.impl.ExamApplication
import de.upb.cs.uc4.exam.impl.actor.ExamBehaviour
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.examreg.DefaultTestExamRegs
import de.upb.cs.uc4.hyperledger.api.model.operation.{ ApprovalList, OperationData, OperationDataState, TransactionInfo }
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, UnsignedProposal }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExamTrait
import de.upb.cs.uc4.operation.OperationServiceStub
import de.upb.cs.uc4.shared.client.JsonUtility._
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.DefaultTestUsers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Base64
import scala.language.reflectiveCalls

/** Tests for the ExamService */
class ExamServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with DefaultTestUsers with DefaultTestExams with DefaultTestExamRegs {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
      new ExamApplication(ctx) with LocalServiceLocator {
        override lazy val courseService: CourseServiceStub = new CourseServiceStub
        override lazy val operationService: OperationServiceStub = new OperationServiceStub
        override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub

        certificateService.setup(lecturer0.username, lecturer1.username)
        courseService.resetToDefaults()

        var exams: Seq[Exam] = Seq()

        override def createHyperledgerActor: ExamBehaviour = new ExamBehaviour(config) {
          override val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
          override val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile_kubernetes_local.yaml")
          override val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

          override val channel: String = "myc"
          override val chaincode: String = "mycc"
          override val caURL: String = ""

          override val adminUsername: String = "cli"
          override val adminPassword: String = ""

          override protected def createConnection: ConnectionExamTrait = new ConnectionExamTrait() {

            override def getProposalAddExam(certificate: String, affiliation: String, examJson: String): (String, Array[Byte]) =
              (OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, examJson.getBytes())

            override def getProposalGetExams(certificate: String, affiliation: String, examIds: Seq[String], courseIds: Seq[String], lecturerIds: Seq[String], moduleIds: Seq[String], types: Seq[String], admittableAt: String, droppableAt: String): (String, Array[Byte]) = ("", Array.empty)

            override def addExam(examJson: String): String = {
              exams :+= examJson.fromJson[Exam]
              examJson
            }

            override def getExams(examIds: Seq[String], courseIds: Seq[String], lecturerIds: Seq[String], moduleIds: Seq[String], types: Seq[String], admittableAt: String, droppableAt: String): String = exams.toJson

            override def getChaincodeVersion: String = "testVersion"

            override val username: String = ""
            override val channel: String = ""
            override val chaincode: String = ""
            override val walletPath: Path = null
            override val networkDescriptionPath: Path = null
          }
        }
      }
    }

  val client: ExamService = server.serviceClient.implement[ExamService]
  val operation: OperationServiceStub = server.application.operationService
  val certificate: CertificateServiceStub = server.application.certificateService
  val course: CourseServiceStub = server.application.courseService

  val moduleId = "mockModuleId"
  val createdCourse: Course = course.addCourse(course0.copy(moduleIds = Seq(moduleId)))
  val examOfLecturer0Modified: Exam = exam0.copy(courseId = createdCourse.courseId, moduleId = moduleId, lecturerEnrollmentId = certificate.get(lecturer0.username).enrollmentId)

  override protected def afterAll(): Unit = server.stop()

  def prepare(exam: Exam*): Unit = {
    server.application.exams ++= exam
  }

  def cleanup(): Unit = {
    server.application.exams = Seq()
  }

  private def asString(unsignedProposal: String) = new String(Base64.getDecoder.decode(unsignedProposal), StandardCharsets.UTF_8)

  "ExamService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    "get proposal add exam" in {
      client.getProposalAddExam.handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke(examOfLecturer0Modified).map {
        unsignedProposalJson: UnsignedProposal =>
          asString(unsignedProposalJson.unsignedProposal).fromJson[Exam] should ===(examOfLecturer0Modified)
      }
    }

    "not get proposal add exam for another lecturer" in {
      client.getProposalAddExam.handleRequestHeader(addAuthorizationHeader(lecturer1.username)).invoke(examOfLecturer0Modified).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "not get proposal add exam with an invalid moduleId" in {
      val invalidExamModified = examOfLecturer0Modified.copy(moduleId = "NonExistingmoduleId")
      client.getProposalAddExam.handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke(invalidExamModified).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
            contain theSameElementsAs Seq("moduleId")
      }
    }
    "not get proposal add exam for a non-existing course" in {

      val invalidExamModified = examOfLecturer0Modified.copy(courseId = "NonExistingCourseId")
      client.getProposalAddExam.handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke(invalidExamModified).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
            contain theSameElementsAs Seq("courseId")
      }
    }

    "get all exams" in {
      prepare(exam0)
      client.getExams(None, None, None, None, None, None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        exams => exams should contain theSameElementsAs Seq(exam0)
      }
    }
  }
}
