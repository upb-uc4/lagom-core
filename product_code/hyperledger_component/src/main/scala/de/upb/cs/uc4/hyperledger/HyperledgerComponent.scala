package de.upb.cs.uc4.hyperledger

import akka.actor.CoordinatedShutdown
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import de.upb.cs.uc4.hyperledger.commands.Shutdown

import scala.concurrent.duration._

trait HyperledgerComponent extends ClusterComponents with LagomConfigComponent {

  val actorFactory: HyperledgerActorFactory[_]

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(actorFactory.companionObject.typeKey)(
      _ => actorFactory.create()
    )
  )

  // Closing HyperLedger connection
  CoordinatedShutdown(actorSystem)
    .addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "shutdownConnection") { () =>
      implicit val timeout: Timeout = Timeout(5.seconds)
      clusterSharding.entityRefFor(actorFactory.companionObject.typeKey, actorFactory.companionObject.entityId).ask(_ => Shutdown())
    }
}
