package de.upb.cs.uc4.hyperledger.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.hyperledger.impl.actor.HyperLedgerBehaviour
import de.upb.cs.uc4.hyperledger.impl.commands.{HyperLedgerCommand, Read, Write}
import de.upb.cs.uc4.shared.client.{CustomException, DetailedError, SimpleError}
import de.upb.cs.uc4.shared.server.messages.{Accepted, Confirmation, Rejected}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class HyperLedgerServiceImpl(clusterSharding: ClusterSharding)(implicit ex: ExecutionContext) extends HyperLedgerService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperLedgerCommand] =
    clusterSharding.entityRefFor(HyperLedgerBehaviour.typeKey, "hl")

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def write(transactionId: String): ServiceCall[Seq[String], Done] = ServiceCall{ params =>
    entityRef.ask[Confirmation](replyTo => Write(transactionId, params, replyTo)).map{
      case Accepted => Done
      case Rejected(reason) => throw new CustomException(TransportErrorCode(500, 1003, "Error"),
        DetailedError("owner mismatch", List[SimpleError](SimpleError("HLService Exception", reason))))
    }
  }

  override def read(transactionId: String): ServiceCall[Seq[String], String] = ServiceCall{ params =>
    entityRef.ask[Option[String]](replyTo => Read(transactionId, params, replyTo)).map{
      case Some(json) => json
      case None => throw new CustomException(TransportErrorCode(404, 1003, "Error"), DetailedError("key not found", List(SimpleError("courseId", "Course id does not exist."))))
    }
  }
}
