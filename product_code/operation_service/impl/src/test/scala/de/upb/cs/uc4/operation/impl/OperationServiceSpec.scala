package de.upb.cs.uc4.operation.impl

import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.hyperledger.api.model.JsonHyperledgerVersion
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionOperationsTrait
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.impl.actor.{ OperationHyperledgerBehaviour, OperationState, WatchlistWrapper }
import de.upb.cs.uc4.operation.impl.commands.{ AddToWatchlist, GetWatchlist, OperationCommand, RemoveFromWatchlist }
import de.upb.cs.uc4.operation.model._
import de.upb.cs.uc4.shared.client.JsonUtility.ToJsonUtil
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, GenericError, UC4Exception }
import de.upb.cs.uc4.shared.client.operation.{ ApprovalList, OperationData, OperationDataState, TransactionInfo }
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.shared.server.messages.Confirmation
import org.hyperledger.fabric.gateway.impl.{ ContractImpl, GatewayImpl }
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Waiters.timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll, BeforeAndAfterEach }

import java.nio.file.Path
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.{ existentials, reflectiveCalls }

/** Tests for the OperationService */
class OperationServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withJdbc()
  ) { ctx =>
      new OperationApplication(ctx) with LocalServiceLocator {

        override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub

        var operationList: Seq[OperationData] = List()

        override def createHyperledgerActor: OperationHyperledgerBehaviour = new OperationHyperledgerBehaviour(config) {

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
                }
                else {
                  operation
                }
              }
              "REJECTED"
            }

            override def getOperations(existingEnrollmentId: String, missingEnrollmentId: String, initiatorEnrollmentId: String, state: String): String = {
              operationList
                .filter(op => state.isEmpty || op.state.toString == state)
                .filter(op => initiatorEnrollmentId.isEmpty || op.initiator == initiatorEnrollmentId)
                .filter(op => missingEnrollmentId.isEmpty || op.missingApprovals.users.contains(missingEnrollmentId) || op.missingApprovals.groups.contains(groups(missingEnrollmentId)))
                .filter(op => existingEnrollmentId.isEmpty || op.existingApprovals.users.contains(existingEnrollmentId))
                .toJson
            }

            override def getOperationData(operationId: String): String = {
              operationList.find(_.operationId == operationId) match {
                case Some(value) => value.toJson
                case None        => throw UC4Exception(404, GenericError(ErrorType.HLNotFound, ""))
              }
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

  val groups: mutable.Map[String, String] = mutable.Map[String, String]()

  val student0: String = "student0"
  val student1: String = "student1"
  val student2: String = "student2"
  val admin0: String = "admin0"
  var operation1: OperationData = _
  var operation2: OperationData = _
  var operation3: OperationData = _

  override protected def beforeAll(): Unit = {
    certificate.setup(student0, student1, student2, admin0)
    groups += certificate.get(student0).enrollmentId -> "Student"
    groups += certificate.get(student1).enrollmentId -> "Student"
    groups += certificate.get(student2).enrollmentId -> "Student"
    groups += certificate.get(admin0).enrollmentId -> "Admin"

    operation1 = OperationData("op1", TransactionInfo("contract", "transaction", ""),
      OperationDataState.PENDING, "", certificate.get(student0).enrollmentId, "", "",
      ApprovalList(Seq(), Seq("Admin")),
      ApprovalList(Seq(certificate.get(student1).enrollmentId), Seq()))
    operation2 = OperationData("op2", TransactionInfo("contract", "transaction", ""),
      OperationDataState.PENDING, "", certificate.get(student1).enrollmentId, "", "",
      ApprovalList(Seq(certificate.get(student2).enrollmentId), Seq()),
      ApprovalList(Seq(), Seq("Admin")))
    operation3 = OperationData("op3", TransactionInfo("contract", "transaction", ""),
      OperationDataState.REJECTED, "", certificate.get(student1).enrollmentId, "", "",
      ApprovalList(Seq(certificate.get(student2).enrollmentId), Seq()),
      ApprovalList(Seq(), Seq("Admin")))
  }

  override protected def afterAll(): Unit = server.stop()

  def prepare(operations: Seq[OperationData]): Unit = {
    server.application.operationList :++= operations
  }

  def cleanup(): Unit = {
    server.application.operationList = List()
  }

  implicit val actorTimeout: Timeout = Timeout(5.seconds)

  def entityRef(username: String): EntityRef[OperationCommand] = {
    server.application.clusterSharding.entityRefFor(OperationState.typeKey, username)
  }

  def prepareWatchlist(username: String, operationId: String): Future[Assertion] = {
    entityRef(username).ask[Confirmation](replyTo => AddToWatchlist(operationId, replyTo))
    eventually(timeout(15.seconds)) {
      entityRef(username).ask[WatchlistWrapper](replyTo => GetWatchlist(replyTo)).map {
        wrapper => wrapper.watchlist should contain(operationId)
      }
    }
  }

  /** Only required when actually using the watchlist in the test */
  def cleanupWatchlist(username: String): Future[Assertion] = {
    entityRef(username).ask[WatchlistWrapper](replyTo => GetWatchlist(replyTo)).flatMap {
      watchlistWrapper =>
        Future.sequence(watchlistWrapper.watchlist.map { entry =>
          entityRef(username).ask[Confirmation](replyTo => RemoveFromWatchlist(entry, replyTo))
        })
    }
    eventually(timeout(15.seconds)) {
      entityRef(username).ask[WatchlistWrapper](replyTo => GetWatchlist(replyTo)).map {
        watchlistWrapper =>
          watchlistWrapper.watchlist.isEmpty should ===(true)
      }
    }
  }

  /** Only required when actually using the watchlist in the test */
  def cleanupOnFailure(username: String): PartialFunction[Throwable, Future[Assertion]] = PartialFunction.fromFunction { exception =>
    cleanupWatchlist(username)
      .map { _ =>
        throw exception
      }
  }

  /** Only required when actually using the watchlist in the test */
  def cleanupOnSuccess(assertion: Assertion, username: String): Future[Assertion] = {
    cleanupWatchlist(username)
      .map { _ =>
        assertion
      }
  }

  override def afterEach(): Unit = cleanup()

  "OperationService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    "get an operation as initiator" in {
      prepare(Seq(operation1))
      client.getOperation(operation1.operationId).handleRequestHeader(addAuthorizationHeader(student0)).invoke().map {
        operation => operation should ===(operation1)
      }
    }

    "get an operation as not the initiator" in {
      prepare(Seq(operation1))
      client.getOperation(operation1.operationId).handleRequestHeader(addAuthorizationHeader(student1)).invoke().map {
        operation => operation should ===(operation1)
      }
    }

    "get an operation with OwnerMismatch" in {
      prepare(Seq(operation1))
      client.getOperation(operation1.operationId).handleRequestHeader(addAuthorizationHeader(student2)).invoke().failed.map {
        ex => ex.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "get operations as admin" in {
      prepare(Seq(operation1, operation2))
      client.getOperations(None, None, None, None).handleRequestHeader(addAuthorizationHeader(admin0)).invoke().map {
        operations => operations should contain theSameElementsAs Seq(operation1, operation2)
      }
    }

    "get operations where the user needs to do actions" in {
      prepare(Seq(operation1, operation2))
      client.getOperations(None, Some(true), None, None).handleRequestHeader(addAuthorizationHeader(admin0)).invoke().map {
        operations => operations should contain theSameElementsAs Seq(operation2)
      }
    }

    "get operations where the user initiated the operation" in {
      prepare(Seq(operation1, operation2))
      client.getOperations(Some(true), None, None, None).handleRequestHeader(addAuthorizationHeader(student0)).invoke().map {
        operations => operations should contain theSameElementsAs Seq(operation1)
      }
    }

    "get operations where the user asks for a state" in {
      prepare(Seq(operation1, operation2, operation3))
      client.getOperations(None, None, Some(OperationDataState.REJECTED.toString), None).handleRequestHeader(addAuthorizationHeader(student1)).invoke().map {
        operations => operations should contain theSameElementsAs Seq(operation3)
      }
    }

    "get operations where the user asks for his watchlist" in {
      prepare(Seq(operation1, operation2, operation3))
      client.getOperations(None, None, None, Some(true)).handleRequestHeader(addAuthorizationHeader(student1)).invoke().map {
        operations => operations should contain theSameElementsAs Seq()
      }
    }

    "reject an operation" in {
      prepare(Seq(operation1, operation2, operation3))
      val reason = "Reasons"
      client.getProposalRejectOperation("op1").handleRequestHeader(addAuthorizationHeader(admin0)).invoke(JsonRejectMessage(reason))
        .map { _ =>
          val rejected = server.application.operationList.find(op => op.operationId == "op1").get
          (rejected.state, rejected.reason) should ===(OperationDataState.REJECTED, reason)
        }
    }

    //WATCHLIST
    "update the watchlist of a user" in {
      prepare(Seq(operation1, operation2))
      client.addToWatchList(student0).handleRequestHeader(addAuthorizationHeader(student0)).invoke(JsonOperationId(operation1.operationId)).flatMap {
        _ =>
          eventually(timeout(Span(15, Seconds))) {
            client.getOperations(None, None, None, Some(true)).handleRequestHeader(addAuthorizationHeader(student0)).invoke().map {
              operations => operations should contain theSameElementsAs Seq(operation1)
            }
          }
      }.flatMap(cleanupOnSuccess(_, student0)).recoverWith(cleanupOnFailure(student0))
    }

    "not update the watchlist of a user as another user" in {
      client.addToWatchList(student0).handleRequestHeader(addAuthorizationHeader(student1)).invoke(JsonOperationId("test")).failed.map {
        ex => ex.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }.flatMap(cleanupOnSuccess(_, student0)).recoverWith(cleanupOnFailure(student0))
    }

    "remove an operation from the watchlist of a user" in {
      prepare(Seq(operation3))
      prepareWatchlist(student1, operation3.operationId).flatMap { _ =>
        client.removeOperation(operation3.operationId).handleRequestHeader(addAuthorizationHeader(student1)).invoke().flatMap { _ =>
          entityRef(student1).ask[WatchlistWrapper](replyTo => GetWatchlist(replyTo)).map {
            answer => answer.watchlist.isEmpty shouldBe true
          }
        }
      }.flatMap(cleanupOnSuccess(_, student1)).recoverWith(cleanupOnFailure(student1))
    }

    "not remove an operation from the watchlist of another user" in {
      prepare(Seq(operation3))
      prepareWatchlist(student1, operation3.operationId).flatMap { _ =>
        client.removeOperation(operation3.operationId).handleRequestHeader(addAuthorizationHeader(student0)).invoke().failed.map {
          ex => ex.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
        }
      }
    }.flatMap(cleanupOnSuccess(_, student1)).recoverWith(cleanupOnFailure(student1))
  }
}
