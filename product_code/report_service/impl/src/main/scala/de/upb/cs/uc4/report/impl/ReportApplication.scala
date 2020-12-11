package de.upb.cs.uc4.report.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.{ ReportBehaviour, ReportState }
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.user.api.UserService
import play.api.db.HikariCPComponents

abstract class ReportApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with LagomKafkaComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[ReportService](wire[ReportServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = ReportSerializerRegistry

  lazy val userService: UserService = serviceClient.implement[UserService]
  lazy val courseService: CourseService = serviceClient.implement[CourseService]
  lazy val matriculationService: MatriculationService = serviceClient.implement[MatriculationService]
  lazy val certificateService: CertificateService = serviceClient.implement[CertificateService]

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(ReportState.typeKey)(
      entityContext => ReportBehaviour.create(entityContext)
    )
  )
}

object ReportApplication {
  val offset: String = "UniversityCredits4Reports"
}

