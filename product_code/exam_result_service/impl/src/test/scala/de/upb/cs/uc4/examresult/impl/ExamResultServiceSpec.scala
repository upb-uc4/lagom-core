package de.upb.cs.uc4.examresult.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.exam.ExamServiceStub
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.examresult.DefaultTestExamResultEntries
import de.upb.cs.uc4.examresult.api.ExamResultService
import de.upb.cs.uc4.examresult.impl.actor.ExamResultBehaviour
import de.upb.cs.uc4.examresult.model.{ ExamResult, ExamResultEntry }
import de.upb.cs.uc4.hyperledger.api.model.operation.{ ApprovalList, OperationData, OperationDataState, TransactionInfo }
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, UnsignedProposal }
import de.upb.cs.uc4.hyperledger.connections.traits.{ ConnectionExamResultTrait, ConnectionExamTrait }
import de.upb.cs.uc4.operation.OperationServiceStub
import de.upb.cs.uc4.shared.client.JsonUtility._
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.DefaultTestUsers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Base64
import scala.language.reflectiveCalls

/** Tests for the ExamResultService */
class ExamResultServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with DefaultTestUsers with DefaultTestExamResultEntries {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
      new ExamResultApplication(ctx) with LocalServiceLocator {
        override lazy val examService: ExamServiceStub = new ExamServiceStub
        override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub
        override lazy val operationService: OperationServiceStub = new OperationServiceStub

        certificateService.setup(lecturer0.username)

        var examResults: Seq[ExamResultEntry] = Seq()

        override def createHyperledgerActor: ExamResultBehaviour = new ExamResultBehaviour(config) {
          override val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
          override val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile_kubernetes_local.yaml")
          override val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

          override val channel: String = "myc"
          override val chaincode: String = "mycc"
          override val caURL: String = ""

          override val adminUsername: String = "cli"
          override val adminPassword: String = ""

          override protected def createConnection: ConnectionExamResultTrait = new ConnectionExamResultTrait {
            override def getProposalAddExamResult(certificate: String, affiliation: String, examResultJson: String): (String, Array[Byte]) =
              (OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, examResultJson.getBytes())

            override def getProposalGetExamResultEntries(certificate: String, affiliation: String, enrollmentId: String, examIds: List[String]): (String, Array[Byte]) = ("", Array.empty)

            override def addExamResult(examJson: String): String = {
              examResults :+= examJson.fromJson[ExamResultEntry]
              examJson
            }

            override def getExamResultEntries(enrollmentId: String, examIds: List[String]): String = examResults.toJson

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

  val client: ExamResultService = server.serviceClient.implement[ExamResultService]
  val operation: OperationServiceStub = server.application.operationService

  override protected def afterAll(): Unit = server.stop()

  def prepare(examResultEntry: ExamResultEntry*): Unit = {
    server.application.examResults ++= examResultEntry
  }

  def cleanup(): Unit = {
    server.application.examResults = Seq()
  }

  private def asString(unsignedProposal: String) = new String(Base64.getDecoder.decode(unsignedProposal), StandardCharsets.UTF_8)

  "ExamResultService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    "get proposal add exam result" in {
      client.getProposalAddExamResult.handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke(ExamResult(Seq(examResultEntry0.copy(enrollmentId = "something")))).map {
        unsignedProposalJson: UnsignedProposal =>
          asString(unsignedProposalJson.unsignedProposal).fromJson[ExamResult].examResultEntries should contain theSameElementsAs Seq(examResultEntry0.copy(enrollmentId = "something"))
      }
    }

    "get all exam results" in {
      prepare(examResultEntry0)
      client.getExamResults(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        examResultsEntries => examResultsEntries should contain theSameElementsAs Seq(examResultEntry0)
      }
    }
  }
}
