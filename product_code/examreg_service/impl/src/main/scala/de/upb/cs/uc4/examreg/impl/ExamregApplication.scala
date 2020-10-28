package de.upb.cs.uc4.examreg.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.actor.{ ExamregBehaviour, ExamregState }
import de.upb.cs.uc4.examreg.impl.readside.{ ExamregDatabase, ExamregEventProcessor }
import de.upb.cs.uc4.shared.server.UC4Application
import play.api.db.HikariCPComponents

abstract class ExamregApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents {

  // Create ReadSide
  lazy val database: ExamregDatabase = wire[ExamregDatabase]
  lazy val processor: ExamregEventProcessor = wire[ExamregEventProcessor]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = ExamregSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[ExamregService](wire[ExamregServiceImpl])

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(ExamregState.typeKey)(
      entityContext => ExamregBehaviour.create(entityContext)
    )
  )
}

object ExamregApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Examreg"
}
