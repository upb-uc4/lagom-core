package de.upb.cs.uc4.examreg.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity }
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.ExamregApplication.refreshCache
import de.upb.cs.uc4.examreg.impl.actor.{ ExamregDatabaseBehaviour, ExamregHyperledgerBehaviour, ExamregState }
import de.upb.cs.uc4.examreg.impl.commands.{ CreateExamregDatabase, GetAllExamregsHyperledger }
import de.upb.cs.uc4.examreg.impl.readside.{ ExamregDatabase, ExamregEventProcessor }
import de.upb.cs.uc4.examreg.model.ExaminationRegulationsWrapper
import de.upb.cs.uc4.hyperledger.impl.HyperledgerComponent
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Rejected }
import org.slf4j.{ Logger, LoggerFactory }
import play.api.db.HikariCPComponents

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

abstract class ExamregApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with HyperledgerComponent {

  protected final val log: Logger = LoggerFactory.getLogger(classOf[ExamregApplication])

  // Create ReadSide
  lazy val database: ExamregDatabase = wire[ExamregDatabase]
  lazy val processor: ExamregEventProcessor = wire[ExamregEventProcessor]

  readSide.register(processor)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  override def createHyperledgerActor: ExamregHyperledgerBehaviour = wire[ExamregHyperledgerBehaviour]

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

  // Schedules the cache refreshing
  val delay: FiniteDuration = config.getInt("uc4.delay.cache").minutes
  actorSystem.scheduler.scheduleAtFixedRate(Duration.Zero, delay)(refreshCache(clusterSharding, log))
}

object ExamregApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Examreg"
  val hlOffset: String = "UniversityCredits4HLExamreg"

  /** Helper method to refresh the examination regulation cache */
  def refreshCache(clusterSharding: ClusterSharding, log: Logger)(implicit timeout: Timeout, ec: ExecutionContext): Runnable = () => {
    clusterSharding.entityRefFor(ExamregHyperledgerBehaviour.typeKey, "Cache")
      .askWithStatus[ExaminationRegulationsWrapper](replyTo => GetAllExamregsHyperledger(replyTo)).map { wrapper =>
        wrapper.examinationRegulations.map { examinationRegulation =>
          clusterSharding.entityRefFor(ExamregState.typeKey, examinationRegulation.name).ask(replyTo => CreateExamregDatabase(examinationRegulation, replyTo)).map {
            case Accepted(_)                  => log.debug("Refreshed Cache of {} ", examinationRegulation.name)
            case Rejected(statusCode, reason) => log.error("Encountered Error during caching.", UC4Exception(statusCode, reason))
          }
        }
      }
  }
}
