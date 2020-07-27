package de.upb.cs.uc4.hyperledger.impl

import java.nio.file.{Path, Paths}

import akka.actor.CoordinatedShutdown
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.hyperledger.ConnectionManager
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.hyperledger.impl.actor.HyperLedgerBehaviour
import de.upb.cs.uc4.hyperledger.impl.commands.Shutdown
import de.upb.cs.uc4.hyperledger.traits.ConnectionManagerTrait
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

import scala.concurrent.duration._

abstract class HyperLedgerApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with ClusterComponents
    with CORSComponents
    with AhcWSComponents {

  lazy val connectionManager: ConnectionManagerTrait = ConnectionManager(
    retrivePath("uc4.hyperledger.networkConfig", "/connection_profile.yaml"),
    retrivePath("uc4.hyperledger.wallet", "/wallet/")
  )

  private def retrivePath(key: String, fallback: String): Path = {
    if (config.hasPath(key)) {
      Paths.get(config.getString(key))
    } else {
      Paths.get(getClass.getResource(fallback).toURI)
    }
  }

  // Set HttpFilter to the default CorsFilter
  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HyperLedgerService](wire[HyperLedgerServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = HyperLedgerSerializerRegistry

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(HyperLedgerBehaviour.typeKey)(
      _ => HyperLedgerBehaviour.create(connectionManager)
    )
  )

  // Closing HyperLedger connection
  CoordinatedShutdown(actorSystem)
    .addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "shutdownConnection") { () =>
      implicit val timeout: Timeout = Timeout(5.seconds)
      clusterSharding.entityRefFor(HyperLedgerBehaviour.typeKey, "hl").ask(_ => Shutdown())
    }
}

object HyperLedgerApplication {
  val cassandraOffset: String = "UniversityCredits4HyperLedger"
}