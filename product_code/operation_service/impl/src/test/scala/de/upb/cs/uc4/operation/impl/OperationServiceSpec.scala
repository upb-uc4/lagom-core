package de.upb.cs.uc4.operation.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.examreg.DefaultTestExamRegs
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.JsonUtil.ToJsonUtil
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionOperationsTrait
import de.upb.cs.uc4.matriculation.MatriculationServiceStub
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.impl.actor.OperationHyperledgerBehaviour
import de.upb.cs.uc4.operation.model.{OperationData, OperationDataState}
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import org.hyperledger.fabric.gateway.impl.{ContractImpl, GatewayImpl}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.nio.file.Path
import scala.language.reflectiveCalls

/** Tests for the MatriculationService */
class OperationServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with DefaultTestExamRegs {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
    new OperationApplication(ctx) with LocalServiceLocator {

      override lazy val matriculationService: MatriculationServiceStub = new MatriculationServiceStub
      override lazy val certificateService: CertificateServiceStub  =new CertificateServiceStub

      var operationList: Seq[OperationData] = List()

      override def createActorFactory: OperationHyperledgerBehaviour = new OperationHyperledgerBehaviour(config) {

        override val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
        override val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile_kubernetes_local.yaml")
        override val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

        override val channel: String = "myc"
        override val chaincode: String = "mycc"
        override val caURL: String = ""

        override val adminUsername: String = "cli"
        override val adminPassword: String = ""

        override protected def createConnection: ConnectionOperationsTrait = new ConnectionOperationsTrait() {

          override def getChaincodeVersion: String = "testVersion"

          override def approveTransaction(initiator: String, contractName: String, transactionName: String, params: String*): String = "APPROVED"

          override def rejectTransaction(operationId: String, rejectMessage: String): String = {
            operationList = operationList.map { operation =>
              if (operation.operationId == operationId) {
                operation.copy(state = OperationDataState.REJECTED, reason = rejectMessage)
              } else {
                operation
              }
            }
            "REJECTED"
          }

          override def getOperations(existingEnrollmentId: String, missingEnrollmentId: String, initiatorEnrollmentId: String, state: String): String = {
            operationList
              .filter(_.state.toString == state)
              .filter(_.initiator == initiatorEnrollmentId)
              .filter(op => op.missingApprovals.user.contains(missingEnrollmentId) || op.missingApprovals.groups.contains(groups(missingEnrollmentId)))
              .filter(op => op.existingApprovals.user.contains(existingEnrollmentId))
              .toJson
          }

          override def getOperationData(operationId: String): String = {
            operationList.filter(_.operationId == operationId).toJson
          }

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

  val client: OperationService = server.serviceClient.implement[OperationService]
  val certificate: CertificateServiceStub = server.application.certificateService
  val matriculation: MatriculationServiceStub = server.application.matriculationService

  val groups = Map[String, String]()

  override protected def afterAll(): Unit = server.stop()

  def prepare(operations: Seq[OperationData]): Unit = {
    server.application.operationList ++= operations
  }

  def cleanup(): Unit = {
    server.application.operationList = List()
  }


  "OperationService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }
  }
}
