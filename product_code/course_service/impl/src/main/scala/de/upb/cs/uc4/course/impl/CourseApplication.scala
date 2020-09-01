package de.upb.cs.uc4.course.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.impl.actor.{ CourseBehaviour, CourseState }
import de.upb.cs.uc4.course.impl.readside.{ CourseDatabase, CourseEventProcessor }
import de.upb.cs.uc4.shared.server.AuthenticationComponent
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

abstract class CourseApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with LagomKafkaComponents
  with CORSComponents
  with AhcWSComponents
  with AuthenticationComponent {

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

object CourseApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Courses"
}
