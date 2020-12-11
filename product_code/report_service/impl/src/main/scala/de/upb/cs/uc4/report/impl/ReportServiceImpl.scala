package de.upb.cs.uc4.report.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.{ Report, ReportState }
import de.upb.cs.uc4.report.impl.commands.{ GetReport, ReportCommand }
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.user.api.UserService
import play.api.Environment
import play.api.libs.json.Json

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

/** Implementation of the ReportService */
class ReportServiceImpl(
    clusterSharding: ClusterSharding,
    userService: UserService,
    courseService: CourseService,
    matriculationService: MatriculationService,
    certificateService: CertificateService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config) extends ReportService {

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[ReportCommand] =
    clusterSharding.entityRefFor(ReportState.typeKey, id)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.database").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  /** Request collection of all data for the given user */
  override def prepareUserData(username: String): ServiceCall[NotUsed, Done] = identifiedAuthenticated(AuthenticationRole.All :_*) {
    (authUsername, role) =>
      ServerServiceCall { (header, _) =>

        if(authUsername != username) {
          throw UC4Exception.OwnerMismatch
        }

        val userFuture = userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
        val certificateFuture = certificateService.getCertificate(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
        val enrollmentIdFuture = certificateService.getEnrollmentId(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
        val encryptedPrivateKeyFuture = certificateService.getPrivateKey(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
        var matriculationFuture : Future[ImmatriculationData] = Future.successful(null)
        var coursesFuture: Future[Seq[Course]] = Future.successful(Nil)

        role match {
          case AuthenticationRole.Admin =>
          case AuthenticationRole.Student =>
            matriculationFuture = matriculationService.getMatriculationData(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
          case AuthenticationRole.Lecturer =>
            coursesFuture = courseService.getAllCourses(lecturerId = Some(username)).handleRequestHeader(addAuthenticationHeader(header)).invoke()
        }

        for {
          user <- userFuture
          certificate <- certificateFuture
          enrollmentId <- enrollmentIdFuture
          encryptedPrivateKey <- encryptedPrivateKeyFuture
          immatriculationData <- matriculationFuture
          courses <- coursesFuture
        } yield {
          val array = Json.toBytes(Json.toJson(user))

        }

        Future.successful(ResponseHeader(200, MessageProtocol.empty, List()), Done)
      }
  }

  /** Get all data for the specified user */
  override def getUserData(username: String): ServiceCall[NotUsed, Array[Byte]] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, _) =>
      ServerServiceCall { (header, _) => {

        if (authUser != username.trim) {
          throw UC4Exception.OwnerMismatch
        }

        entityRef(authUser).ask[Option[Report]](replyTo => GetReport(replyTo)).map {
          case Some(Report(array, timestamp)) =>
            createETagHeader(header, array, headers=List(("Last-Modified", timestamp)))
          case None =>
            throw UC4Exception.PreconditionRequired
        }
      }
    }
  }

  /** Allows GET */
  override def allowedMethodsGET: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")


}
