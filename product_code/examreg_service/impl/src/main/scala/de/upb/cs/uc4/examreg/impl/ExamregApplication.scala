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
import de.upb.cs.uc4.examreg.impl.commands.{ CreateExamregDatabase, CreateExamregHyperledger }
import de.upb.cs.uc4.examreg.impl.readside.{ ExamregDatabase, ExamregEventProcessor }
import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }
import de.upb.cs.uc4.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import org.slf4j.{ Logger, LoggerFactory }
import play.api.db.HikariCPComponents

import scala.concurrent.duration._

abstract class ExamregApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with HyperledgerComponent {

  private final val log: Logger = LoggerFactory.getLogger(classOf[ExamregApplication])

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
  val defaultExamRegs: Seq[ExaminationRegulation] = Seq(
    ExaminationRegulation(
      "Bachelor Computer Science v3",
      active = true,
      Seq(
        Module("M.1275.01158", "Math 1"),
        Module("M.1275.01159", "Math 2"),
        Module("M.1275.78235", "Complexity Theory")
      )
    ),
    ExaminationRegulation(
      "Bachelor Computer Science v4",
      active = true,
      Seq(
        Module("M.1278.15686", "Math"),
        Module("M.1275.78235", "Complexity Theory"),
        Module("M.1278.79512", "IT-Security")
      )
    ),
    ExaminationRegulation(
      "Bachelor Philosophy v1",
      active = true,
      Seq(
        Module("M.1358.15686", "Introduction to Philosophy"),
        Module("M.1358.15653", "Theoretical Philosophy"),
        Module("M.1358.15418", "Applied Ethics")
      )
    )
  )
  defaultExamRegs.foreach { defaultExamReg =>
    clusterSharding.entityRefFor(ExamregHyperledgerBehaviour.typeKey, ExamregHyperledgerBehaviour.entityId)
      .askWithStatus[Confirmation](replyTo => CreateExamregHyperledger(defaultExamReg, replyTo)).map {
        case Accepted(_) => clusterSharding.entityRefFor(ExamregState.typeKey, defaultExamReg.name)
          .ask[Confirmation](replyTo => CreateExamregDatabase(defaultExamReg, replyTo)).map {
            case Accepted(_)                  => log.info(s"Default examreg ${defaultExamReg.name} created successfully")
            case Rejected(statusCode, reason) => log.error(s"Failed database operation of creating default examreg ${defaultExamReg.name}", UC4Exception(statusCode, reason))
          }
        case Rejected(statusCode, reason) => log.error(s"Failed hyperledger operation of creating default examreg ${defaultExamReg.name}", UC4Exception(statusCode, reason))
      }.recover {
        case ue: UC4Exception => log.error(s"Failed operation of creating default examreg ${defaultExamReg.name}", ue)
        case t: Throwable     => log.error("Error in Exam application", t)
      }
  }
}

object ExamregApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Examreg"
  val hlOffset: String = "UniversityCredits4HLExamreg"
}
