package de.upb.cs.uc4.report.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.{ ByteString, Timeout }
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, RequestHeader, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.admission.model.CourseAdmission
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.{ ReportState, ReportStateEnum, ReportWrapper, TextReport }
import de.upb.cs.uc4.report.impl.commands._
import de.upb.cs.uc4.shared.client.exceptions.{ UC4Exception, _ }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import de.upb.cs.uc4.user.api.UserService
import net.lingala.zip4j.ZipFile
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment
import play.api.libs.json.Json

import java.io.{ ByteArrayInputStream, FileWriter }
import java.nio.file.{ Files, Paths }
import java.util.Calendar
import javax.imageio.ImageIO
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
    admissionService: AdmissionService,
    operationService: OperationService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, timeout: Timeout) extends ReportService {

  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[ReportCommand] =
    clusterSharding.entityRefFor(ReportState.typeKey, id)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  /** Request collection of all data for the given user */
  def prepareUserData(username: String, role: AuthenticationRole, header: RequestHeader, timestamp: String): Future[Done] = Future {

    val userFuture = userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
      case exception: Exception =>
        log.error(s"Prepare of $username at $timestamp ; userFuture failed", exception)
        throw exception
    }
    val profilePictureFuture = userService.getImage(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
      case exception: Exception =>
        log.error(s"Prepare of $username at $timestamp ; profilePictureFuture failed", exception)
        throw exception
    }
    val thumbnailFuture = userService.getThumbnail(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
      case exception: Exception =>
        log.error(s"Prepare of $username at $timestamp ; thumbnailFuture failed", exception)
        throw exception
    }
    val certificateFuture = certificateService.getCertificate(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
      .map(cert => Some(cert.certificate)).recover { case _ => None }
    val enrollmentIdFuture = certificateService.getEnrollmentId(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
      case exception: Exception =>
        log.error(s"Prepare of $username at $timestamp ; enrollmentIdFuture failed", exception)
        throw exception
    }
    val encryptedPrivateKeyFuture = certificateService.getPrivateKey(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
      .map(Some(_)).recover { case _ => None }
    var matriculationFuture: Future[Option[ImmatriculationData]] = Future.successful(None)
    var coursesFuture: Future[Option[Seq[Course]]] = Future.successful(None)
    var admissionFuture: Future[Option[Seq[CourseAdmission]]] = Future.successful(None)

    role match {
      case AuthenticationRole.Admin =>
      case AuthenticationRole.Student =>
        matriculationFuture = matriculationService.getMatriculationData(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_)).recover {
          case ue: UC4Exception if ue.possibleErrorResponse.`type` == ErrorType.HLNotFound || ue.possibleErrorResponse.`type` == ErrorType.KeyNotFound =>
            None
          case exception: Exception =>
            log.error(s"Prepare of $username at $timestamp ; matriculationFuture failed", exception)
            throw exception
        }
        admissionFuture = admissionService.getCourseAdmissions(Some(username), None, None).handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_)).recover {
          case ue: UC4Exception if ue.possibleErrorResponse.`type` == ErrorType.HLNotFound || ue.possibleErrorResponse.`type` == ErrorType.KeyNotFound =>
            None
          case exception: Exception =>
            log.error(s"Prepare of $username at $timestamp ; admissionFuture failed", exception)
            throw exception
        }
      case AuthenticationRole.Lecturer =>
        coursesFuture = courseService.getAllCourses(None, Some(username), None).handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_)).recover {
          case exception: Exception =>
            log.error(s"Prepare of $username at $timestamp ; coursesFuture failed", exception)
            throw exception
        }
    }

    val operationFuture = operationService.getOperations(Some(false), Some(false), Some(""), Some(false))
      .handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
        case exception: Exception =>
          log.error(s"Prepare of $username at $timestamp ; operationFuture failed", exception)
          throw exception
      }

    val watchlistFuture = operationService.getWatchlist(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
      .recover {
        case exception: Exception =>
          log.error(s"Prepare of $username at $timestamp ; operationFuture failed", exception)
          throw exception
      }

    for {
      user <- userFuture
      profilePicture <- profilePictureFuture
      thumbnail <- thumbnailFuture
      certificate <- certificateFuture
      jsonEnrollmentId <- enrollmentIdFuture
      encryptedPrivateKey <- encryptedPrivateKeyFuture
      immatriculationData <- matriculationFuture
      courses <- coursesFuture
      admissions <- admissionFuture
      operations <- operationFuture
      watchlist <- watchlistFuture
    } yield {
      val report = TextReport(user, certificate, jsonEnrollmentId.id, encryptedPrivateKey,
        immatriculationData, courses, admissions, operations, watchlist, timestamp)
      entityRef(username).ask[Confirmation](replyTo => SetReport(report, profilePicture.toArray, thumbnail.toArray, timestamp, replyTo)).map {
        case Accepted(_) =>
        case Rejected(statusCode, reason) =>
          log.error(s"Report of $username can't be persisted.", UC4Exception(statusCode, reason))
      }.recover {
        case exception: Exception => log.error(s"Prepare of $username at $timestamp ; Actor communication failed", exception)
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
        val textReport = reportWrapper.textReport
        val zipPath = Paths.get(System.getProperty("java.io.tmpdir"), s"report_$username", "report.zip")
        val zipFile = new ZipFile(zipPath.toFile)

        val folderPath = Paths.get(System.getProperty("java.io.tmpdir"), s"report_$username")
        val reportPath = Paths.get(folderPath.toString, "userdata.json")
        val profilePicturePath = Paths.get(folderPath.toString, "profilePicture.jpeg")
        val thumbnailPath = Paths.get(folderPath.toString, "thumbnail.jpeg")

        if (!folderPath.toFile.exists()) {
          folderPath.toFile.mkdirs()
        }

        val reportTxt = Json.prettyPrint(Json.toJson(textReport))

        var array: Array[Byte] = null

        Using(new FileWriter(reportPath.toFile)) { writer =>
          writer.write(reportTxt)
        }

        val profilePicture = ImageIO.read(new ByteArrayInputStream(reportWrapper.profilePicture.get))
        ImageIO.write(profilePicture, "jpeg", profilePicturePath.toFile)

        val thumbnail = ImageIO.read(new ByteArrayInputStream(reportWrapper.thumbnail.get))
        ImageIO.write(thumbnail, "jpeg", thumbnailPath.toFile)

        zipFile.addFile(reportPath.toFile)
        zipFile.addFile(profilePicturePath.toFile)
        zipFile.addFile(thumbnailPath.toFile)

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
                  prepareUserData(username, authRole, header, timestamp)
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
              }.recover {
                case ex: Exception =>
                  log.error(s"Report of $username can't be zipped.", ex)
                  throw UC4Exception.InternalServerError("Unknown Internal Error when trying to zip report", ex.getMessage, ex)
              }
          }
        }
      }
  }

  /** Delete a user's report */
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
