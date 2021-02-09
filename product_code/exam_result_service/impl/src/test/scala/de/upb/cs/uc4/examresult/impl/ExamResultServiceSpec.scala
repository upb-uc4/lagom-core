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

import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception }

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

        certificateService.setup(lecturer0.username, lecturer1.username, student0.username, student1.username)
        examService.setup()

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

            override def getExamResultEntries(enrollmentId: String, examIds: List[String]): String = examResults
              .filter(exam => examIds.isEmpty || examIds.contains(exam.examId))
              .filter(exam => enrollmentId.isEmpty || enrollmentId == exam.enrollmentId).toJson

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

  val allExamResults = Seq(exam0ResultEntry0, exam0ResultEntry1, exam0ResultEntry2, exam1ResultEntry0, exam1ResultEntry1, exam1ResultEntry2, exam3ResultEntry0, exam3ResultEntry1, exam3ResultEntry2)
  val allExam0Results: Seq[ExamResultEntry] = allExamResults.filter(_.examId == exam0.examId)
  val allExam1Results: Seq[ExamResultEntry] = allExamResults.filter(_.examId == exam1.examId)
  val allExam3Results: Seq[ExamResultEntry] = allExamResults.filter(_.examId == exam3.examId)

  override protected def afterAll(): Unit = server.stop()

  def prepare(examResultEntry: ExamResultEntry*): Unit = {
    cleanup()
    server.application.examResults ++= examResultEntry
  }
  def prepareSeq(examResultEntry: Seq[ExamResultEntry]): Unit = {
    cleanup()
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
      client.getProposalAddExamResult.handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke(ExamResult(Seq(exam0ResultEntry0.copy(enrollmentId = "something")))).map {
        unsignedProposalJson: UnsignedProposal =>
          asString(unsignedProposalJson.unsignedProposal).fromJson[ExamResult].examResultEntries should contain theSameElementsAs Seq(exam0ResultEntry0.copy(enrollmentId = "something"))
      }
    }
    "not get proposal add exam result with an invalid examResult" in {
      client.getProposalAddExamResult.handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke(ExamResult(Seq(exam0ResultEntry0.copy(enrollmentId = ""), exam0ResultEntry1.copy(examId = ""), exam0ResultEntry2.copy(grade = "Failed")))).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
            contain theSameElementsAs Seq("examResultEntries[0].enrollmentId", "examResultEntries[1].examId", "examResultEntries[2].grade")
      }
    }

    "get all exam results" in {
      prepareSeq(allExamResults)
      client.getExamResults(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        examResultsEntries => examResultsEntries should contain theSameElementsAs allExamResults
      }
    }

    "get a students exam results as the student himself" in {
      prepareSeq(allExamResults)
      client.getExamResults(Some(student0.username), None).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map {
        examResultsEntries => examResultsEntries should contain theSameElementsAs allExamResults.filter(_.enrollmentId == student0.username + "enrollmentId")
      }
    }
    "not get the exam results of another student" in {
      prepareSeq(allExamResults)
      client.getExamResults(Some(student0.username), None).handleRequestHeader(addAuthorizationHeader(student1.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "get all exam results from an exam hold by the lecture himself" in {
      prepareSeq(allExamResults)
      client.getExamResults(None, Some(exam3ResultEntry0.examId)).handleRequestHeader(addAuthorizationHeader(lecturer1.username)).invoke().map {
        examResultsEntries => examResultsEntries should contain theSameElementsAs allExam3Results
      }
    }

    "not get all exam results as a lecturer" in {
      prepareSeq(allExamResults)
      client.getExamResults(None, None).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "not get an exam result from another lecturer" in {
      prepareSeq(allExamResults)
      client.getExamResults(None, Some(exam0ResultEntry0.examId)).handleRequestHeader(addAuthorizationHeader(lecturer1.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }
  }
}
