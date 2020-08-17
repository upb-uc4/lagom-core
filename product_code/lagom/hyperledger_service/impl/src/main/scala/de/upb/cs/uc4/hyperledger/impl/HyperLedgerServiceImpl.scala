package de.upb.cs.uc4.hyperledger.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.{ ByteString, Timeout }
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.deser.RawExceptionMessage
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, TransportErrorCode }
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.hyperledger.exceptions.TransactionException
import de.upb.cs.uc4.hyperledger.exceptions.traits.{ HyperledgerExceptionTrait, TransactionExceptionTrait }
import de.upb.cs.uc4.hyperledger.impl.actor.HyperLedgerBehaviour
import de.upb.cs.uc4.hyperledger.impl.commands.{ HyperLedgerCommand, Read, Write }
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.server.ServiceCallFactory
import play.api.Environment

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

class HyperLedgerServiceImpl(clusterSharding: ClusterSharding)(implicit ex: ExecutionContext) extends HyperLedgerService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperLedgerCommand] =
    clusterSharding.entityRefFor(HyperLedgerBehaviour.typeKey, "hl")

  implicit val timeout: Timeout = Timeout(60.seconds)

  override def write(transactionId: String): ServiceCall[Seq[String], Done] = ServiceCall { params =>
    entityRef.ask[Try[Done]](replyTo => Write(transactionId, params, replyTo)).map {
      case Success(Done)      => Done
      case Failure(exception) => throw hlExceptionDeserialize(exception)
    }
  }

  override def read(transactionId: String): ServiceCall[Seq[String], String] = ServiceCall { params =>
    entityRef.ask[Try[String]](replyTo => Read(transactionId, params, replyTo)).map {
      case Success(json)      => json
      case Failure(exception) => throw hlExceptionDeserialize(exception)
    }
  }

  def hlExceptionDeserialize(exception: Throwable): CustomException = {
    exception match {
      case transactionException: TransactionExceptionTrait =>
        errorMatching(new CustomExceptionSerializer(Environment.simple()).deserialize(
          RawExceptionMessage(TransportErrorCode(418, 1003, "Error"), MessageProtocol.empty, ByteString.fromString(transactionException.jsonError))
        ).asInstanceOf[CustomException])
      case hyperledgerException: HyperledgerExceptionTrait => throw new CustomException(500, InformativeError("hl: internal error", hyperledgerException.innerException.toString))
      case _ => throw CustomException.InternalServerError
    }
  }

  def errorMatching(customException: CustomException): CustomException = {
    customException.getPossibleErrorResponse match {
      case genericError: GenericError => genericError.`type` match {
        case "hl: unknown transactionId"      => new CustomException(500, genericError)
        case "hl: unprocessable entity"       => new CustomException(422, genericError)
        case "hl: not found"                  => new CustomException(404, genericError)
        case "hl: conflict"                   => new CustomException(409, genericError)
        case "hl: unprocessable ledger state" => new CustomException(500, genericError)
      }
      case detailedError: DetailedError => detailedError.`type` match {
        case "hl: unprocessable field" => new CustomException(422, detailedError)
      }
      case transactionError: TransactionError => transactionError.`type` match {
        case "hl: invalid transaction call" => new CustomException(500, transactionError)
      }
    }
  }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCallFactory.allowedMethodsCustom("GET")
}
