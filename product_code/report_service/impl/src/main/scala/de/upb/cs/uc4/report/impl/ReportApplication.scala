package de.upb.cs.uc4.report.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examresult.api.ExamResultService
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.pdf.api.PdfProcessingService
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.{ ReportBehaviour, ReportState }
import de.upb.cs.uc4.report.impl.commands.DeleteReport
import de.upb.cs.uc4.shared.client.JsonUsername
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionComponent
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.api.UserService
import org.slf4j.{ Logger, LoggerFactory }
import play.api.db.HikariCPComponents

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

abstract class ReportApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with LagomKafkaComponents
  with KafkaEncryptionComponent {

  protected final val log: Logger = LoggerFactory.getLogger(classOf[ReportApplication])

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[ReportService](wire[ReportServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = ReportSerializerRegistry

  lazy val userService: UserService = serviceClient.implement[UserService]
  lazy val courseService: CourseService = serviceClient.implement[CourseService]
  lazy val matriculationService: MatriculationService = serviceClient.implement[MatriculationService]
  lazy val certificateService: CertificateService = serviceClient.implement[CertificateService]
  lazy val admissionService: AdmissionService = serviceClient.implement[AdmissionService]
  lazy val examregService: ExamregService = serviceClient.implement[ExamregService]
  lazy val examService: ExamService = serviceClient.implement[ExamService]
  lazy val examResultService: ExamResultService = serviceClient.implement[ExamResultService]
  lazy val pdfService: PdfProcessingService = serviceClient.implement[PdfProcessingService]

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(ReportState.typeKey)(
      entityContext => ReportBehaviour.create(entityContext)
    )
  )

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.database").milliseconds)

  userService
    .userDeletionTopicMinimal()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[EncryptionContainer, Future[Done]] { container =>
        try {
          val jsonUsername = kafkaEncryptionUtility.decrypt[JsonUsername](container)
          clusterSharding.entityRefFor(ReportState.typeKey, jsonUsername.username)
            .ask[Confirmation](replyTo => DeleteReport(replyTo)).map(_ => Done)
        }
        catch {
          case throwable: Throwable =>
            log.error("ReportService received invalid topic message: {}", throwable.toString)
            Future.successful(Done)
        }
      }
        .mapAsync(8)(done => done)
    )
}

object ReportApplication {
  val offset: String = "UniversityCredits4Reports"
}

