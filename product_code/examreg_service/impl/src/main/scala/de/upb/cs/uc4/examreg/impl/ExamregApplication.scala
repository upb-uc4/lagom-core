package de.upb.cs.uc4.examreg.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.actor.{ ExamregDatabaseBehaviour, ExamregHyperledgerBehaviour, ExamregState }
import de.upb.cs.uc4.examreg.impl.readside.{ ExamregDatabase, ExamregEventProcessor }
import de.upb.cs.uc4.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.shared.server.UC4Application
import play.api.db.HikariCPComponents

import scala.concurrent.duration._

abstract class ExamregApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with HyperledgerComponent {

  // Create ReadSide
  lazy val database: ExamregDatabase = wire[ExamregDatabase]
  lazy val processor: ExamregEventProcessor = wire[ExamregEventProcessor]

  readSide.register(processor)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  override def createActorFactory: ExamregHyperledgerBehaviour = wire[ExamregHyperledgerBehaviour]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = ExamregSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[ExamregService](wire[ExamregServiceImpl])

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(ExamregState.typeKey)(
      entityContext => ExamregDatabaseBehaviour.create(entityContext)
    )
  )
}

object ExamregApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Examreg"
  val hlOffset: String = "UniversityCredits4HLExamreg"
}
