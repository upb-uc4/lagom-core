package de.upb.cs.uc4.report.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.{ ByteString, Timeout }
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, RequestHeader, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.admission.model.{ CourseAdmission, ExamAdmission }
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.examresult.api.ExamResultService
import de.upb.cs.uc4.examresult.model.ExamResultEntry
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.pdf.api.PdfProcessingService
import de.upb.cs.uc4.pdf.model.PdfProcessor
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.{ ReportState, ReportStateEnum, ReportWrapper, TextReport }
import de.upb.cs.uc4.report.impl.commands._
import de.upb.cs.uc4.report.impl.signature.SigningService
import de.upb.cs.uc4.shared.client.Utils
import de.upb.cs.uc4.shared.client.Utils.SemesterUtils
import de.upb.cs.uc4.shared.client.exceptions.{ UC4Exception, _ }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.user.Student
import net.lingala.zip4j.ZipFile
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment
import play.api.libs.json.Json

import java.io.{ ByteArrayInputStream, File, FileWriter }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }
import java.util.{ Base64, Calendar }
import javax.imageio.ImageIO
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source
import scala.reflect.io.Directory
import scala.util.{ Failure, Success, Try, Using }

/** Implementation of the ReportService */
class ReportServiceImpl(
    clusterSharding: ClusterSharding,
    userService: UserService,
    courseService: CourseService,
    matriculationService: MatriculationService,
    certificateService: CertificateService,
    admissionService: AdmissionService,
    operationService: OperationService,
    examService: ExamService,
    examResultService: ExamResultService,
    pdfService: PdfProcessingService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, timeout: Timeout) extends ReportService {

  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[ReportCommand] =
    clusterSharding.entityRefFor(ReportState.typeKey, id)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  private lazy val absoluteCertificateEnrollmentHtml = new File(
    Try {
      config.getString("uc4.pdf.certificateEnrollmentHtml")
    } match {
      case Success(path) => path
      case Failure(ex) =>
        log.error("Absolut path to certificateEnrollmentHtml invalid or not defined", ex)
        ""
    }
  )

  lazy val certificateEnrollmentHtml: String = Try {
    if (absoluteCertificateEnrollmentHtml.exists()) {
      Using(Source.fromFile(absoluteCertificateEnrollmentHtml)) { source =>
        source.getLines().mkString("\n")
      }.getOrElse("")
    }
    else {
      Source.fromResource("certificateEnrollment.html").getLines().mkString("\n")
    }
  } match {
    case Success(html) => html
    case Failure(ex) =>
      log.error("Error when trying to load certificateEnrollmentHtml", ex)
      ""
  }

  lazy val signatureService = new SigningService(
    config.getString("uc4.pdf.keyStorePath"),
    config.getString("uc4.pdf.keyStorePassword"),
    config.getString("uc4.pdf.certificateAlias"),
    config.getString("uc4.pdf.tsaURL")
  )

  /** Request collection of all data for the given user */
  def prepareUserData(username: String, role: AuthenticationRole, header: RequestHeader, timestamp: String): Future[Done] = {

    certificateService.getEnrollmentIds(Some(username)).handleRequestHeader(addAuthenticationHeader(header)).invoke().map { enrollmentIdPair =>

      val enrollmentId = enrollmentIdPair.head.enrollmentId

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

      val encryptedPrivateKeyFuture = certificateService.getPrivateKey(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
        .map(Some(_)).recover { case _ => None }
      var matriculationFuture: Future[Option[ImmatriculationData]] = Future.successful(None)
      var coursesFuture: Future[Option[Seq[Course]]] = Future.successful(None)
      var courseAdmissionFuture: Future[Option[Seq[CourseAdmission]]] = Future.successful(None)
      var examAdmissionFuture: Future[Option[Seq[ExamAdmission]]] = Future.successful(None)
      var examFuture: Future[Option[Seq[Exam]]] = Future.successful(None)
      var examResultFuture: Future[Option[Seq[ExamResultEntry]]] = Future.successful(None)

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
          courseAdmissionFuture = admissionService.getCourseAdmissions(Some(username), None, None).handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_)).recover {
            case ue: UC4Exception if ue.possibleErrorResponse.`type` == ErrorType.HLNotFound || ue.possibleErrorResponse.`type` == ErrorType.KeyNotFound =>
              None
            case exception: Exception =>
              log.error(s"Prepare of $username at $timestamp ; courseAdmissionFuture failed", exception)
              throw exception
          }
          examAdmissionFuture = admissionService.getExamAdmissions(Some(username), None, None)
            .handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_)).recover {
              case exception: Exception =>
                log.error(s"Prepare of $username at $timestamp ; examAdmissionFuture failed", exception)
                throw exception
            }
          examResultFuture = examResultService.getExamResults(Some(username), None)
            .handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_)).recover {
              case exception: Exception =>
                log.error(s"Prepare of $username at $timestamp ; examResultFuture failed", exception)
                throw exception
            }

        case AuthenticationRole.Lecturer =>
          coursesFuture = courseService.getAllCourses(None, Some(username), None).handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_)).recover {
            case exception: Exception =>
              log.error(s"Prepare of $username at $timestamp ; coursesFuture failed", exception)
              throw exception
          }
          examFuture = examService.getExams(None, None, Some(enrollmentId), None, None, None, None)
            .handleRequestHeader(addAuthenticationHeader(header)).invoke().map(Some(_)).recover {
              case exception: Exception =>
                log.error(s"Prepare of $username at $timestamp ; examFuture failed", exception)
                throw exception
            }
      }

      val operationFuture = operationService.getOperations(None, None, None, None)
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
        encryptedPrivateKey <- encryptedPrivateKeyFuture
        immatriculationData <- matriculationFuture
        courses <- coursesFuture
        operations <- operationFuture
        watchlist <- watchlistFuture
        courseAdmissions <- courseAdmissionFuture
        examAdmissions <- examAdmissionFuture
        exams <- examFuture
        examResults <- examResultFuture
      } yield {
        val report = TextReport(user, certificate, enrollmentId, encryptedPrivateKey,
          immatriculationData,
          courses, courseAdmissions, examAdmissions, exams, examResults, operations, watchlist, timestamp)
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
      .recover {
        case exception: Exception =>
          log.error(s"Prepare of $username at $timestamp ; enrollmentIdFuture failed", exception)
          throw exception
      }
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
                  prepareUserData(authUser, authRole, header, timestamp)
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

  /** Returns a pdf with the certificate of enrollment */
  override def getCertificateOfEnrollment(username: String, semesterBase64: Option[String]): ServiceCall[NotUsed, ByteString] =
    identifiedAuthenticated(AuthenticationRole.Student) {
      (authUsername, _) =>
        ServerServiceCall { (header, _) =>
          if (authUsername != username) {
            throw UC4Exception.OwnerMismatch
          }
          var pdf = certificateEnrollmentHtml

          userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { user =>
            matriculationService.getMatriculationData(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { data: ImmatriculationData =>

              val semester = if (semesterBase64.isDefined) {
                new String(Base64.getUrlDecoder.decode(semesterBase64.get), StandardCharsets.UTF_8)
              }
              else {
                Utils.findLatestSemester(data.matriculationStatus.flatMap(_.semesters))
              }

              val validationList = semester.validateSemester

              if (validationList.nonEmpty) {
                throw UC4Exception(422, DetailedError(ErrorType.Validation, validationList))
              }

              val enrollmentExists = data.matriculationStatus.map(subjectMatriculation => subjectMatriculation.semesters.contains(semester)).reduce(_ || _)
              if (!enrollmentExists) {
                throw UC4Exception.NotFound
              }

              pdf = pdf.replace("{studentName}", s"${user.firstName}  ${user.lastName}")
              pdf = pdf.replace("{matriculationId}", user.asInstanceOf[Student].matriculationId)
              pdf = pdf.replace("{enrollmentId}", data.enrollmentId)
              pdf = pdf.replace("{semester}", semester)
              pdf = pdf.replace("{startDate}", Utils.semesterToStartDate(semester))
              pdf = pdf.replace("{endDate}", Utils.semesterToEndDate(semester))
              pdf = pdf.replace("{address}", config.getString("uc4.pdf.address").replace("\n", "<br>"))
              pdf = pdf.replace("{organization}", config.getString("uc4.pdf.organization"))
              pdf = pdf.replace("{semesterCount}", data.matriculationStatus.flatMap(_.semesters).distinct.size.toString)

              pdf = pdf.replace("{subjectList}", data.matriculationStatus.filter(_.semesters.contains(semester))
                .map(subj => s"<li>${subj.fieldOfStudy} (${subj.semesters.size})</li>").mkString("\n"))

              pdfService.convertHtml().invoke(PdfProcessor(pdf)).map { pdfBytes =>
                val output = signatureService.signPdf(pdfBytes.toArray)

                (ResponseHeader(200, MessageProtocol(contentType = Some("application/pdf")), List()), ByteString(output))
              }
            }
          }
        }
    }

  /** Allows GET */
  override def allowedMethodsGETDELETE: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, DELETE")

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
