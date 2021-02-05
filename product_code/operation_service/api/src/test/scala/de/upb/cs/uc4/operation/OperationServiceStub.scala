package de.upb.cs.uc4.operation

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.hyperledger.api.model._
import de.upb.cs.uc4.hyperledger.api.model.operation.OperationData
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.model.{ JsonOperationId, JsonRejectMessage }

import scala.concurrent.Future

class OperationServiceStub extends OperationService {
  /** Returns the Operation for the matching operationId */
  override def getOperation(operationId: String): ServiceCall[NotUsed, OperationData] = _ => Future.successful(null)

  /** Returns the Operations for the matching filters */
  override def getOperations(selfInitiated: Option[Boolean], selfActionRequired: Option[Boolean], states: Option[String], watchlistOnly: Option[Boolean]): ServiceCall[NotUsed, Seq[OperationData]] = _ => Future.successful(Seq())

  /** Remove an Operation from watchlist */
  override def removeOperation(operationId: String): ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

  /** Approve the operation with the given operationId */
  override def getProposalApproveOperation(operationId: String): ServiceCall[NotUsed, UnsignedProposal] = _ => Future.successful(UnsignedProposal(""))

  /** Reject the operation with the given operationId */
  override def getProposalRejectOperation(operationId: String): ServiceCall[JsonRejectMessage, UnsignedProposal] = _ => Future.successful(UnsignedProposal(""))

  /** Gets the watchlist of a user */
  override def getWatchlist(username: String): ServiceCall[NotUsed, Seq[String]] = _ => Future.successful(Seq())

  /** Adds a operationId to the watchlist */
  override def addToWatchList(username: String): ServiceCall[JsonOperationId, Done] = _ => Future.successful(Done)

  /** Submit a signed Proposal */
  override def submitProposal(): ServiceCall[SignedProposal, UnsignedTransaction] = _ => Future.successful(UnsignedTransaction(""))

  /** Submit a signed Proposal */
  override def submitTransaction(): ServiceCall[SignedTransaction, Done] = _ => Future.successful(Done)

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

  /** Allows GET, DELETE */
  override def allowedGetDelete: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

  /** Allows POST */
  override def allowedGetPost: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = _ => Future.successful(JsonHyperledgerVersion("invalid", "invalid"))

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

}
