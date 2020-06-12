package de.upb.cs.uc4.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.api.CourseService
import de.upb.cs.uc4.impl.actor.{CourseBehaviour, CourseState}
import de.upb.cs.uc4.impl.readside.{CourseDatabase, CourseEventProcessor}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

abstract class CourseApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with CORSComponents
    with AhcWSComponents {

  // Create ReadSide
  lazy val database: CourseDatabase = wire[CourseDatabase]
  lazy val processor: CourseEventProcessor = wire[CourseEventProcessor]

  // Set HttpFilter to the default CorsFilter
  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[CourseService](wire[CourseServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = CourseSerializerRegistry

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(CourseState.typeKey)(
      entityContext => CourseBehaviour.create(entityContext)
    )
  )
}

object CourseApplication{
  /** Functions as offset for the database */
  val cassandraOffset: String = "UniversityCredits4Courses"
}