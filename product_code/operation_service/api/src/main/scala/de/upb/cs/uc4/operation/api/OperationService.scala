package de.upb.cs.uc4.operation.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.operation.model.{ JsonRejectMessage, OperationData }
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The OperationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the OperationService.
  */
trait OperationService extends UC4HyperledgerService {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  override val pathPrefix: String = "/operation-management"
  /** The name of the service */
  override val name: String = "operation"

  /** Returns the Operation for the matching operationId */
  def getOperation(operationId: String): ServiceCall[NotUsed, OperationData]

  /** Returns the Operations for the matching filters */
  def getOperations(selfInitiated: Option[Boolean], selfActionRequired: Option[Boolean], states: Option[String], watchlistOnly: Option[Boolean]): ServiceCall[NotUsed, Seq[OperationData]]

  /** Remove an Operation from watchlist */
  def removeOperation(operationId: String): ServiceCall[NotUsed, Done]

  /** Reject the operation with the given operationId*/
  def getProposalRejectOperation(operationId: String): ServiceCall[JsonRejectMessage, UnsignedProposal]

  /** Submit a signed Proposal */
  def submitProposal(operationId: String): ServiceCall[SignedProposal, UnsignedTransaction]

  /** Submit a signed Proposal */
  def submitTransaction(operationId: String): ServiceCall[SignedTransaction, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows GET, DELETE*/
  def allowedGetDelete: ServiceCall[NotUsed, Done]

  /** Allows POST */
  def allowedPost: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/operation?selfInitiated&selfActionRequired&states&watchlistOnly", getOperations _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/operation?selfInitiated&selfActionRequired&states&watchlistOnly", allowedGet _),

        restCall(Method.GET, pathPrefix + "/operation/:operationId", getOperation _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.DELETE, pathPrefix + "/operation/:operationId", removeOperation _),
        restCall(Method.OPTIONS, pathPrefix + "/operation/:operationId", allowedGetDelete _),

        restCall(Method.POST, pathPrefix + "/operation/:operationId/unsigned_proposal_reject", getProposalRejectOperation _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.POST, pathPrefix + "/operation/:operationId/signed_proposal", submitProposal _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.POST, pathPrefix + "/operation/:operationId/signed_transaction", submitTransaction _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/operation/:operationId/unsigned_proposal_reject", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/operation/:operationId/signed_proposal", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/operation/:operationId/signed_transaction", allowedPost _)
      )
  }
}
