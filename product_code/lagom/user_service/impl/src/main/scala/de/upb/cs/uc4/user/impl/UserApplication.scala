package de.upb.cs.uc4.user.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.shared.server.AuthenticationComponent
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.{UserBehaviour, UserState}
import de.upb.cs.uc4.user.impl.readside.{UserDatabase, UserEventProcessor}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents


abstract class UserApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with CORSComponents
    with AhcWSComponents
    with AuthenticationComponent {

  // Create ReadSide
  lazy val database: UserDatabase = wire[UserDatabase]
  lazy val processor: UserEventProcessor = wire[UserEventProcessor]

  // Set HttpFilter to the default CorsFilter
  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[UserService](wire[UserServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = UserSerializerRegistry

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(UserState.typeKey)(
      entityContext => UserBehaviour.create(entityContext)
    )
  )
}

object UserApplication{
  /** Functions as offset for the database */
  val cassandraOffset: String = "UniversityCredits4Users"
}
