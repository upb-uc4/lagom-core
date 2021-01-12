package de.upb.cs.uc4.operation.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.stream.Materializer
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.hyperledger.HyperledgerUtils
import de.upb.cs.uc4.hyperledger.commands.HyperledgerBaseCommand
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.impl.actor.OperationHyperledgerBehaviour
import de.upb.cs.uc4.operation.model.{ OperationData, RejectMessageJson }
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import play.api.Environment

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** Implementation of the OperationService */
class OperationServiceImpl(
    clusterSharding: ClusterSharding,
    matriculationService: MatriculationService,
    certificateService: CertificateService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends OperationService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(OperationHyperledgerBehaviour.typeKey, OperationHyperledgerBehaviour.entityId)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  /** Returns the Operations for the matching operationId */
  override def getOperation(operationId: String): ServiceCall[NotUsed, OperationData] = ???

  /** Returns the Operations for the matching filters */
  override def getOperations(selfInitiated: Option[Boolean], selfActionRequired: Option[Boolean], states: Option[String], watchlistOnly: Option[Boolean]): ServiceCall[NotUsed, Seq[OperationData]] = ???

  /** Remove an Operation from watchlist */
  override def removeOperation(operationId: String): ServiceCall[NotUsed, Done] = ???

  // TODO: For now just rejects, without returning a proposal. Change into proper frontend signing
  /** Returns a proposal for rejecting the operation with the given operationId */
  override def getProposalRejectOperation(operationId: String): ServiceCall[RejectMessageJson, UnsignedProposal] = ???

  /** Submit a signed Proposal */
  override def submitProposal(operationId: String): ServiceCall[SignedProposal, UnsignedTransaction] = ServiceCall {
    _ => throw UC4Exception.NotImplemented
  }

  /** Submit a signed Proposal */
  override def submitTransaction(operationId: String): ServiceCall[SignedTransaction, Done] = ServiceCall {
    _ => throw UC4Exception.NotImplemented
  }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows GET, DELETE */
  override def allowedGetDelete: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, DELETE")

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = ServiceCall { _ =>
    HyperledgerUtils.VersionUtil.createHyperledgerVersionResponse(entityRef)
  }
}
