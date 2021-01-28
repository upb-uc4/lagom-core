package de.upb.cs.uc4.operation.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.stream.Materializer
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.hyperledger.api.model.operation.{ OperationData, OperationDataState }
import de.upb.cs.uc4.hyperledger.api.model._
import de.upb.cs.uc4.hyperledger.impl.HyperledgerUtils
import de.upb.cs.uc4.hyperledger.impl.commands.{ HyperledgerBaseCommand, SubmitProposal, SubmitTransaction }
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.impl.actor.{ OperationHyperledgerBehaviour, OperationState }
import de.upb.cs.uc4.operation.impl.commands._
import de.upb.cs.uc4.operation.model.{ JsonOperationId, JsonRejectMessage }
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import play.api.Environment

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

/** Implementation of the OperationService */
class OperationServiceImpl(
    clusterSharding: ClusterSharding,
    certificateService: CertificateService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends OperationService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(OperationHyperledgerBehaviour.typeKey, OperationHyperledgerBehaviour.entityId)

  private def entityRef(username: String): EntityRef[OperationCommand] =
    clusterSharding.entityRefFor(OperationState.typeKey, username)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  /** Returns the Operation for the matching operationId */
  override def getOperation(operationId: String): ServiceCall[NotUsed, OperationData] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, role) =>
      ServerServiceCall { (header, _) =>
        entityRef.askWithStatus(replyTo => GetOperationHyperledger(operationId, replyTo))
          .recover(handleException("Get operation"))
          .flatMap {
            operationData =>
              certificateService.getEnrollmentId(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke().map {
                jsonEnrollmentId =>
                  if (role != AuthenticationRole.Admin && !operationData.isInvolved(jsonEnrollmentId.id, role.toString)) {
                    throw UC4Exception.OwnerMismatch
                  }
                  createETagHeader(header, operationData)
              }
          }
      }
  }

  /** Returns the Operations for the matching filters */
  override def getOperations(selfInitiated: Option[Boolean], selfActionRequired: Option[Boolean], states: Option[String], watchlistOnly: Option[Boolean]): ServiceCall[NotUsed, Seq[OperationData]] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, role) =>
      ServerServiceCall { (header, _) =>
        certificateService.getEnrollmentId(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { jsonId =>

          val missingEnrollmentId = if (selfActionRequired.isDefined && selfActionRequired.get) {
            jsonId.id
          }
          else {
            ""
          }

          val initiatorEnrollmentId = if (selfInitiated.isDefined && selfInitiated.get) {
            jsonId.id
          }
          else {
            ""
          }

          val involvedEnrollmentId = if (role == AuthenticationRole.Admin) {
            ""
          }
          else {
            jsonId.id
          }

          val stateSeq = if (states.isDefined) {
            states.get.split(",").toSeq
          }
          else {
            Seq()
          }

          entityRef.askWithStatus(replyTo =>
            GetOperationsHyperledger(
              Seq(),
              "", // Unused filter
              missingEnrollmentId,
              initiatorEnrollmentId,
              involvedEnrollmentId,
              stateSeq,
              replyTo
            )).recover(handleException("Get operations"))
            //Involved filter
            .map(_.operationDataList)
            //Watchlist filter
            .flatMap { operations =>
              if (watchlistOnly.isDefined && watchlistOnly.get) {
                entityRef(authUser).ask(replyTo => GetWatchlist(replyTo)).map(_.watchlist).map { ids =>
                  operations.filter(op => ids.contains(op.operationId))
                }
              }
              else {
                Future.successful(operations)
              }
            }
            //Finish
            .map {
              operations => createETagHeader(header, operations)
            }
        }
      }
  }

  /** Remove an Operation from the watchlist */
  override def removeOperation(operationId: String): ServiceCall[NotUsed, Done] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, _) =>
      ServerServiceCall { (header, _) =>
        getOperation(operationId).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap {
          operationData =>
            certificateService.getEnrollmentId(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap {
              jsonEnrollmentId =>
                if (operationData.initiator != jsonEnrollmentId.id) {
                  throw UC4Exception.OwnerMismatch
                }
                if (operationData.initiator == jsonEnrollmentId.id && operationData.state == OperationDataState.PENDING) {
                  throw UC4Exception.RemovalNotAllowed
                }
                entityRef(authUser).ask(replyTo => RemoveFromWatchlist(operationId, replyTo)).map {
                  case Accepted(_)                  => (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                  case Rejected(statusCode, reason) => throw UC4Exception(statusCode, reason)
                }
            }
        }
      }
  }

  /** Approve the operation with the given operationId */
  override def getProposalApproveOperation(operationId: String): ServiceCall[NotUsed, UnsignedProposal] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, _) =>
      ServerServiceCall { (header, _) =>
        certificateService.getCertificate(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { jsonCertificate =>
          entityRef.askWithStatus(replyTo =>
            GetProposalApproveOperationHyperledger(
              jsonCertificate.certificate,
              operationId,
              replyTo
            )).recover(handleException("Approve operation")).map { unsignedProposal =>
            (ResponseHeader(200, MessageProtocol.empty, List()), unsignedProposal)
          }
        }
      }
  }

  /** Returns a proposal for rejecting the operation with the given operationId */
  override def getProposalRejectOperation(operationId: String): ServiceCall[JsonRejectMessage, UnsignedProposal] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, _) =>
      ServerServiceCall { (header, jsonRejectMessage) =>
        certificateService.getCertificate(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { jsonCertificate =>
          entityRef.askWithStatus(replyTo =>
            GetProposalRejectOperationHyperledger(
              jsonCertificate.certificate,
              operationId,
              jsonRejectMessage.rejectMessage,
              replyTo
            )).recover(handleException("Reject operation")).map { unsignedProposal =>
            (ResponseHeader(200, MessageProtocol.empty, List()), unsignedProposal)
          }
        }
      }
  }

  /** Adds a operationId to the watchlist */
  def addToWatchList(username: String): ServiceCall[JsonOperationId, Done] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, _) =>
      ServerServiceCall { (_, jsonOperationId) =>
        if (authUser != username.trim) {
          throw UC4Exception.OwnerMismatch
        }
        entityRef(authUser).ask[Confirmation](replyTo => AddToWatchlist(jsonOperationId.id, replyTo)).map {
          case Accepted(_)                  => (ResponseHeader(200, MessageProtocol.empty, List()), Done)
          case Rejected(statusCode, reason) => throw UC4Exception(statusCode, reason)
        }
      }
  }

  /** Submit a signed Proposal */
  override def submitProposal(operationId: String): ServiceCall[SignedProposal, UnsignedTransaction] =
    authenticated[SignedProposal, UnsignedTransaction](AuthenticationRole.All: _*) {
      ServerServiceCall { (_, signedProposal) =>
        entityRef.askWithStatus[Array[Byte]](replyTo => SubmitProposal(signedProposal, replyTo)).map { array =>
          (ResponseHeader(200, MessageProtocol.empty, List()), UnsignedTransaction(array))
        }.recover(handleException("Submit proposal failed"))
      }
    }

  /** Submit a signed Transaction */
  override def submitTransaction(operationId: String): ServiceCall[SignedTransaction, Done] =
    authenticated[SignedTransaction, Done](AuthenticationRole.All: _*) {
      ServerServiceCall { (_, signedTransaction) =>
        entityRef.askWithStatus[Confirmation](replyTo => SubmitTransaction(signedTransaction, replyTo)).map {
          case Accepted(_) =>
            (ResponseHeader(202, MessageProtocol.empty, List()), Done)
          case Rejected(statusCode, reason) =>
            throw UC4Exception(statusCode, reason)
        }.recover(handleException("Submit transaction failed"))
      }
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
