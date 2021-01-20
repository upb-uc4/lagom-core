package de.upb.cs.uc4.admission.impl

import akka.Done
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.admission.impl.actor.AdmissionBehaviour
import de.upb.cs.uc4.admission.model.{ CourseAdmission, DropAdmission }
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.course.{ CourseServiceStub, DefaultTestCourses }
import de.upb.cs.uc4.examreg.{ DefaultTestExamRegs, ExamregServiceStub }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionAdmissionTrait
import de.upb.cs.uc4.matriculation.MatriculationServiceStub
import de.upb.cs.uc4.shared.client.JsonUtility._
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.client.operation.{ ApprovalList, OperationData, OperationDataState, TransactionInfo }
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

        certificateService.setup(student0.username)
        matriculationService.addImmatriculationData(student0.username, matriculationService.createSingleImmatriculationData(
          certificateService.get(student0.username).enrollmentId, examReg0.name, "SS2020"
        ))

        var admissionList: Seq[CourseAdmission] = List()

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

            override def getProposalGetAdmission(certificate: String, affiliation: String, enrollmentId: String, courseId: String, moduleId: String): (String, Array[Byte]) = ("", Array.empty)

            override def getAdmissions(enrollmentId: String, courseId: String, moduleId: String): String = {
              Json.stringify(Json.toJson(
                admissionList
                  .filter(admission => enrollmentId == "" || admission.enrollmentId == enrollmentId)
                  .filter(admission => courseId == "" || admission.courseId == courseId)
                  .filter(admission => moduleId == "" || admission.moduleId == moduleId)
              ))
            }

            override def getProposalAddAdmission(certificate: String, AFFILITATION: String = AFFILIATION, courseAdmission: String): (String, Array[Byte]) =
              (OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, courseAdmission.getBytes())

            override def getProposalDropAdmission(certificate: String, AFFILITATION: String = AFFILIATION, admissionId: String): (String, Array[Byte]) =
              (OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, ("id#" + admissionId).getBytes())

            override def getUnsignedTransaction(proposalBytes: Array[Byte], signature: Array[Byte]): Array[Byte] = {
              "t:".getBytes() ++ proposalBytes
            }

            override def submitSignedTransaction(transactionBytes: Array[Byte], signature: Array[Byte]): (String, String) = {
              new String(transactionBytes, StandardCharsets.UTF_8) match {
                case s"t:id#$admissionId" =>
                  admissionList = admissionList.filter(admission => admission.admissionId != admissionId)
                  ("", "")
                case s"t:$courseAdmission" =>
                  admissionList :+= courseAdmission.fromJson[CourseAdmission]
                  ("", "")
              }
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
          }
        }
      }
    }

  val client: AdmissionService = server.serviceClient.implement[AdmissionService]
  val certificate: CertificateServiceStub = server.application.certificateService
  private var defaultCourse = course0.copy(moduleIds = examReg0.modules.map(_.id))
  defaultCourse = server.application.courseService.addCourse(defaultCourse)
  private val defaultAdmission = CourseAdmission("", defaultCourse.courseId, defaultCourse.moduleIds.head, "", "")

  override protected def afterAll(): Unit = server.stop()

  override protected def afterEach(): Unit = cleanup()

  def prepare(admissions: CourseAdmission*): Unit = {
    server.application.admissionList ++= admissions
  }

  def cleanup(): Unit = {
    server.application.admissionList = List()
  }

  private def asString(unsignedProposal: String) = new String(Base64.getDecoder.decode(unsignedProposal), StandardCharsets.UTF_8)

  "AdmissionService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    "get course admission" in {
      prepare(defaultAdmission)
      client.getCourseAdmissions(None, None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        admissions => admissions should contain theSameElementsAs Seq(defaultAdmission)
      }
    }

    "get proposal add course admission" in {
      client.getProposalAddCourseAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultAdmission).map {
        unsignedProposalJson =>
          asString(unsignedProposalJson.unsignedProposal).fromJson[CourseAdmission]
            .copy(timestamp = "") should ===(defaultAdmission.copy(enrollmentId = certificate.get(student0.username).enrollmentId))
      }
    }

    "get proposal drop course admission" in {
      client.getProposalDropCourseAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(DropAdmission("id")).map {
        unsignedProposalJson =>
          asString(unsignedProposalJson.unsignedProposal) should ===("id#id")
      }
    }

    "submit proposal add course admission" in {
      client.getProposalAddCourseAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultAdmission).flatMap {
        unsignedProposalJson =>
          client.submitProposal().handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(SignedProposal(unsignedProposalJson.unsignedProposal, "")).map {
            unsignedTransactionJson =>
              asString(unsignedTransactionJson.unsignedTransaction) match {
                case s"t:$admission" => admission.fromJson[CourseAdmission].copy(timestamp = "") should ===(
                  defaultAdmission.copy(enrollmentId = certificate.get(student0.username).enrollmentId)
                )
              }
          }
      }
    }

    "submit proposal drop course admission" in {
      client.getProposalDropCourseAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(DropAdmission("id")).flatMap {
        unsignedProposalJson =>
          client.submitProposal().handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(SignedProposal(unsignedProposalJson.unsignedProposal, "")).map {
            unsignedTransactionJson =>
              asString(unsignedTransactionJson.unsignedTransaction) should ===("t:id#id")
          }
      }
    }

    "submit transaction add course admission" in {
      client.getProposalAddCourseAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultAdmission).flatMap {
        unsignedProposalJson =>
          client.submitProposal().handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(SignedProposal(unsignedProposalJson.unsignedProposal, "")).flatMap {
            unsignedTransactionJson =>
              client.submitTransaction().handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(SignedTransaction(unsignedTransactionJson.unsignedTransaction, "")).map {
                answer => answer should ===(Done)
              }
          }
      }
    }

    "submit transaction drop course admission" in {
      client.getProposalDropCourseAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(DropAdmission("id")).flatMap {
        unsignedProposalJson =>
          client.submitProposal().handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(SignedProposal(unsignedProposalJson.unsignedProposal, "")).flatMap {
            unsignedTransactionJson =>
              client.submitTransaction().handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(SignedTransaction(unsignedTransactionJson.unsignedTransaction, "")).map {
                answer => answer should ===(Done)
              }
          }
      }
    }
  }
}
