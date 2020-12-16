package de.upb.cs.uc4.report.impl

import java.io.FileWriter
import java.nio.file.{ Files, Paths }
import java.util.Calendar

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.{ ByteString, Timeout }
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, RequestHeader, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.{ Report, ReportState, ReportStateEnum, ReportWrapper }
import de.upb.cs.uc4.report.impl.commands._
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import de.upb.cs.uc4.user.api.UserService
import net.lingala.zip4j.ZipFile
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.io.Directory
import scala.util.Using

/** Implementation of the ReportService */
class ReportServiceImpl(
    clusterSharding: ClusterSharding,
    userService: UserService,
    courseService: CourseService,
    matriculationService: MatriculationService,
    certificateService: CertificateService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, timeout: Timeout) extends ReportService {

  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[ReportCommand] =
    clusterSharding.entityRefFor(ReportState.typeKey, id)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  /** Request collection of all data for the given user */
  def prepareUserData(username: String, role: AuthenticationRole, header: RequestHeader): Future[Done] = Future {

    val userFuture = userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
    val certificateFuture = certificateService.getCertificate(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
      .map(cert => Some(cert.certificate)).recover { case _ => None }
    val enrollmentIdFuture = certificateService.getEnrollmentId(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
    val encryptedPrivateKeyFuture = certificateService.getPrivateKey(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
      .map(Some(_)).recover { case _ => None }
    var matriculationFuture: Future[Option[ImmatriculationData]] = Future.successful(None)
    var coursesFuture: Future[Option[Seq[Course]]] = Future.successful(None)

    role match {
      case AuthenticationRole.Admin =>
      case AuthenticationRole.Student =>
        matriculationFuture = matriculationService.getMatriculationData(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_))
      case AuthenticationRole.Lecturer =>
        coursesFuture = courseService.getAllCourses(None, Some(username), None).handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_))
    }

    for {
      user <- userFuture
      certificate <- certificateFuture
      jsonEnrollmentId <- enrollmentIdFuture
      encryptedPrivateKey <- encryptedPrivateKeyFuture
      immatriculationData <- matriculationFuture
      courses <- coursesFuture
    } yield {
      val report = Report(user, certificate, jsonEnrollmentId.id, encryptedPrivateKey, immatriculationData, courses)
      entityRef(username).ask[Confirmation](replyTo => SetReport(report, replyTo)).map {
        case Accepted(_) =>
        case Rejected(statusCode, reason) =>
          log.error(s"Report of $username can't be persisted.", UC4Exception(statusCode, reason))
      }
    }.recover {
      case exception: Exception => log.error(s"Report of $username can't be created.", exception)
    }

    Done
  }

  /** Get all data for the specified user */
  def convertReadyReportToZip(username: String): Future[ByteString] =
    entityRef(username).ask[ReportWrapper](replyTo => GetReport(replyTo)).map {
      reportWrapper =>
        val report = reportWrapper
        val zipPath = Paths.get(System.getProperty("java.io.tmpdir"), s"report_$username", "report.zip")
        val zipFile = new ZipFile(zipPath.toFile)

        val folderPath = Paths.get(System.getProperty("java.io.tmpdir"), s"report_$username")
        val reportPath = Paths.get(folderPath.toString, "userdata.json")

        if (!folderPath.toFile.exists()) {
          folderPath.toFile.mkdirs()
        }

        val reportTxt = Json.prettyPrint(Json.toJson(report))
        var array: Array[Byte] = null

        Using(new FileWriter(reportPath.toFile)) { writer =>
          writer.write(reportTxt)
        }

        zipFile.addFile(reportPath.toFile)

        array = Files.readAllBytes(zipFile.getFile.toPath)

        val directory = new Directory(folderPath.toFile)
        directory.deleteRecursively()

        ByteString(array)
    }

  /** Get all data for the specified user */
  override def getUserReport(username: String): ServiceCall[NotUsed, ByteString] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, authRole) =>
      ServerServiceCall { (header, _) =>
        if (authUser != username.trim) {
          throw UC4Exception.OwnerMismatch
        }
        val ref = entityRef(authUser)
        ref.ask[ReportWrapper](replyTo => GetReport(replyTo)).flatMap { reportWrapper =>
          reportWrapper.state match {
            case ReportStateEnum.None =>
              val timestamp = Calendar.getInstance().getTime.toString
              ref.ask[Confirmation](replyTo => PrepareReport(timestamp, replyTo)).map {
                case Accepted(_) =>
                  prepareUserData(username, authRole, header)
                  (
                    ResponseHeader(202, MessageProtocol.empty, List())
                    .addHeader("X-UC4-Timestamp", timestamp),
                    ByteString.empty
                  )

                case Rejected(statusCode, reason) => throw UC4Exception(statusCode, reason)
              }
            case ReportStateEnum.Preparing =>
              Future.successful(
                ResponseHeader(202, MessageProtocol.empty, List())
                  .addHeader("X-UC4-Timestamp", reportWrapper.timestamp.get),
                ByteString.empty
              )
            case ReportStateEnum.Ready =>
              convertReadyReportToZip(username).map {
                zippedBytes =>
                  (
                    ResponseHeader(200, MessageProtocol(contentType = Some("application/zip")), List())
                    .addHeader("X-UC4-Timestamp", reportWrapper.timestamp.get)
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate"),
                    zippedBytes
                  )
              }
          }
        }
      }
  }

  override def deleteUserReport(username: String): ServiceCall[NotUsed, Done] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, _) =>
      ServerServiceCall { (_, _) =>
        if (authUser != username.trim) {
          throw UC4Exception.OwnerMismatch
        }
        entityRef(authUser).ask[Confirmation](replyTo => DeleteReport(replyTo)).map {
          case Accepted(_)                  => (ResponseHeader(200, MessageProtocol(), List()), Done)
          case Rejected(statusCode, reason) => throw UC4Exception(statusCode, reason)
        }
      }
  }

  /** Allows GET */
  override def allowedMethodsGETDELETE: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, DELETE")

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

}
