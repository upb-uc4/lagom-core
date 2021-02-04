package de.upb.cs.uc4.hyperledger.impl

import akka.actor.CoordinatedShutdown
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import de.upb.cs.uc4.hyperledger.impl.commands.{ Activation, Shutdown }
import play.api.libs.concurrent.AkkaComponents

import scala.concurrent.duration._

trait HyperledgerComponent extends ClusterComponents with LagomConfigComponent with AkkaComponents {

  /** Creates the HyperledgerActor for this application.
    * Should not be called manually.
    * If a reference to the actor is needed use [[hyperledgerActor]].
    *
    * @return the actor for this application
    */
  protected def createHyperledgerActor: HyperledgerActor[_]

  /** The HyperledgerActor of this application */
  lazy val hyperledgerActor: HyperledgerActor[_] = createHyperledgerActor

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(hyperledgerActor.companionObject.typeKey)(
      _ => hyperledgerActor.create()
    )
  )

  clusterSharding.entityRefFor(hyperledgerActor.companionObject.typeKey, hyperledgerActor.companionObject.entityId).tell(Activation())

  // Closing HyperLedger connection
  CoordinatedShutdown(actorSystem)
    .addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "shutdownConnection") { () =>
      implicit val timeout: Timeout = Timeout(5.seconds)
      clusterSharding.entityRefFor(hyperledgerActor.companionObject.typeKey, hyperledgerActor.companionObject.entityId).ask(_ => Shutdown())
    }
}
