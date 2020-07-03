package de.upb.cs.uc4.hyperledger.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.hyperledger.impl.actor.HyperLedgerBehaviour
import de.upb.cs.uc4.hyperledger.impl.commands.{HyperLedgerCommand, Read, Write}
import de.upb.cs.uc4.shared.messages.{Accepted, Confirmation, Rejected}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class HyperLedgerServiceImpl(clusterSharding: ClusterSharding)(implicit ex: ExecutionContext) extends HyperLedgerService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperLedgerCommand] =
    clusterSharding.entityRefFor(HyperLedgerBehaviour.typeKey, "hl")

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def write(): ServiceCall[String, Done] = ServiceCall{ json =>
    entityRef.ask[Confirmation](replyTo => Write(json, replyTo)).map{
      case Accepted => Done
      case Rejected(reason) => throw BadRequest(reason)
    }
  }

  override def read(key: String): ServiceCall[NotUsed, String] = ServiceCall{ _ =>
    entityRef.ask[Option[String]](replyTo => Read(key, replyTo)).map{
      case Some(json) => json
      case None => throw NotFound("Does not exists.")
    }
  }
}
