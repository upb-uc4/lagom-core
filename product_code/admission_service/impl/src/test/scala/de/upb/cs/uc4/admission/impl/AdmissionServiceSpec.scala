package de.upb.cs.uc4.admission.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.admission.impl.actor.AdmissionBehaviour
import de.upb.cs.uc4.admission.model.{ AdmissionType, CourseAdmission, DropAdmission, ExamAdmission }
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.course.{ CourseServiceStub, DefaultTestCourses }
import de.upb.cs.uc4.examreg.{ DefaultTestExamRegs, ExamregServiceStub }
import de.upb.cs.uc4.hyperledger.api.model
import de.upb.cs.uc4.hyperledger.api.model.JsonHyperledgerVersion
import de.upb.cs.uc4.hyperledger.api.model.operation.{ ApprovalList, OperationData, OperationDataState, TransactionInfo }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionAdmissionTrait
import de.upb.cs.uc4.matriculation.MatriculationServiceStub
import de.upb.cs.uc4.operation.OperationServiceStub
import de.upb.cs.uc4.shared.client.JsonUtility._
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.DefaultTestUsers
import org.hyperledger.fabric.gateway.impl.{ ContractImpl, GatewayImpl }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Base64
import scala.language.reflectiveCalls

/** Tests for the AdmissionService */
class AdmissionServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with DefaultTestUsers with DefaultTestCourses with DefaultTestExamRegs with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
      new AdmissionApplication(ctx) with LocalServiceLocator {
        override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub
        override lazy val matriculationService: MatriculationServiceStub = new MatriculationServiceStub
        override lazy val examRegService: ExamregServiceStub = new ExamregServiceStub
        override lazy val courseService: CourseServiceStub = new CourseServiceStub
        override lazy val operationService: OperationServiceStub = new OperationServiceStub

        certificateService.setup(student0.username)
        matriculationService.addImmatriculationData(student0.username, matriculationService.createSingleImmatriculationData(
          certificateService.get(student0.username).enrollmentId, examReg0.name, "SS2020"
        ))

        var courseAdmissionList: Seq[CourseAdmission] = List()
        var examAdmissionList: Seq[ExamAdmission] = List()

        override def createHyperledgerActor: AdmissionBehaviour = new AdmissionBehaviour(config) {

          override val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
          override val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile_kubernetes_local.yaml")
          override val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

          override val channel: String = "myc"
          override val chaincode: String = "mycc"
          override val caURL: String = ""

          override val adminUsername: String = "cli"
          override val adminPassword: String = ""

          override protected def createConnection: ConnectionAdmissionTrait = new ConnectionAdmissionTrait() {

            override def getProposalGetAdmissions(certificate: String, affiliation: String = AFFILIATION, enrollmentId: String = "", courseId: String = "", moduleId: String = ""): (String, Array[Byte]) = ("", Array.empty)

            override def getAdmissions(enrollmentId: String, courseId: String, moduleId: String): String = {
              Json.stringify(Json.toJson(
                courseAdmissionList
                  .filter(admission => enrollmentId == "" || admission.enrollmentId == enrollmentId)
                  .filter(admission => courseId == "" || admission.courseId == courseId)
                  .filter(admission => moduleId == "" || admission.moduleId == moduleId)
              ))
            }

            override def getProposalAddAdmission(certificate: String, AFFILITATION: String = AFFILIATION, courseAdmission: String): (String, Array[Byte]) =
              (OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, courseAdmission.getBytes())

            override def getProposalDropAdmission(certificate: String, AFFILITATION: String = AFFILIATION, admissionId: String): (String, Array[Byte]) =
              (model.operation.OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, ("id#" + admissionId).getBytes())

            override def getUnsignedTransaction(proposalBytes: Array[Byte], signature: Array[Byte]): Array[Byte] = {
              "t:".getBytes() ++ proposalBytes
            }

            override def getChaincodeVersion: String = "testVersion"

            override def addAdmission(admission: String): String = ""

            override def dropAdmission(admissionId: String): String = ""

            override lazy val contract: ContractImpl = null
            override lazy val gateway: GatewayImpl = null
            override val username: String = ""
            override val channel: String = ""
            override val chaincode: String = ""
            override val walletPath: Path = null
            override val networkDescriptionPath: Path = null

            override def getProposalGetCourseAdmissions(certificate: String, affiliation: String, enrollmentId: String, courseId: String, moduleId: String): (String, Array[Byte]) = ???

            override def getProposalGetExamAdmissions(certificate: String, affiliation: String, admissionIds: List[String], enrollmentId: String, examIds: List[String]): (String, Array[Byte]) = ???

            override def getCourseAdmissions(enrollmentId: String, courseId: String, moduleId: String): String = ???

            override def getExamAdmissions(admissionIds: List[String], enrollmentId: String, examIds: List[String]): String = ???
          }
        }
      }
    }

  val client: AdmissionService = server.serviceClient.implement[AdmissionService]
  val certificate: CertificateServiceStub = server.application.certificateService
  private var defaultCourse = course0.copy(moduleIds = examReg0.modules.map(_.id))
  defaultCourse = server.application.courseService.addCourse(defaultCourse)
  private val defaultCourseAdmission = CourseAdmission("", "", "", AdmissionType.Course.toString, defaultCourse.courseId, defaultCourse.moduleIds.head)

  override protected def afterAll(): Unit = server.stop()

  override protected def afterEach(): Unit = cleanup()

  def prepare(admissions: CourseAdmission*): Unit = {
    server.application.courseAdmissionList ++= admissions
  }

  def cleanup(): Unit = {
    server.application.courseAdmissionList = List()
  }

  private def asString(unsignedProposal: String) = new String(Base64.getDecoder.decode(unsignedProposal), StandardCharsets.UTF_8)

  "AdmissionService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    "get course admission" in {
      prepare(defaultCourseAdmission)
      client.getCourseAdmissions(None, None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        admissions => admissions should contain theSameElementsAs Seq(defaultCourseAdmission)
      }
    }

    "get proposal add course admission" in {
      client.getProposalAddCourseAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultCourseAdmission).map {
        unsignedProposalJson =>
          asString(unsignedProposalJson.unsignedProposal).fromJson[CourseAdmission]
            .copy(timestamp = "") should ===(defaultCourseAdmission.copy(enrollmentId = certificate.get(student0.username).enrollmentId))
      }
    }

    "get proposal drop course admission" in {
      client.getProposalDropAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(DropAdmission("id")).map {
        unsignedProposalJson =>
          asString(unsignedProposalJson.unsignedProposal) should ===("id#id")
      }
    }
  }
}
