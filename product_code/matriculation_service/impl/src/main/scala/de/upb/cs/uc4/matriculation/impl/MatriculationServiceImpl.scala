package de.upb.cs.uc4.matriculation.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.stream.Materializer
import akka.util.{ ByteString, Timeout }
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, UnsignedProposal }
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerBaseCommand
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerUtils, ProposalWrapper }
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.matriculation.impl.commands._
import de.upb.cs.uc4.matriculation.impl.signature.SigningService
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation }
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.model.JsonOperationId
import de.upb.cs.uc4.pdf.api.PdfProcessingService
import de.upb.cs.uc4.pdf.model.PdfProcessor
import de.upb.cs.uc4.shared.client.Utils
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.user.Student
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment

import java.io.File
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, TimeoutException }
import scala.io.Source
import scala.util.Using

/** Implementation of the MatriculationService */
class MatriculationServiceImpl(
    clusterSharding: ClusterSharding,
    pdfService: PdfProcessingService,
    userService: UserService,
    examregService: ExamregService,
    certificateService: CertificateService,
    operationService: OperationService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends MatriculationService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(MatriculationBehaviour.typeKey, MatriculationBehaviour.entityId)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  private final val log: Logger = LoggerFactory.getLogger(classOf[MatriculationServiceImpl])

  private lazy val absoluteCertificateEnrollmentHtml = new File(config.getString("uc4.pdf.certificateEnrollmentHtml"))
  lazy val certificateEnrollmentHtml: String = if (absoluteCertificateEnrollmentHtml.exists()) {
    Using(Source.fromFile(absoluteCertificateEnrollmentHtml)) { source =>
      source.getLines().mkString("\n")
    }.getOrElse("")
  }
  else {
    Source.fromResource("certificateEnrollment.html").getLines().mkString("\n")
  }

  lazy val signatureService = new SigningService(
    config.getString("uc4.pdf.keyStorePath"),
    config.getString("uc4.pdf.keyStorePassword"),
    config.getString("uc4.pdf.certificateAlias"),
    config.getString("uc4.pdf.tsaURL")
  )

  /** Get proposal to matriculate a student */
  override def getMatriculationProposal(username: String): ServiceCall[PutMessageMatriculation, UnsignedProposal] =
    identifiedAuthenticated[PutMessageMatriculation, UnsignedProposal](AuthenticationRole.Student, AuthenticationRole.Admin) { (authUser, authRole) =>
      ServerServiceCall { (header, rawMatriculationProposal) =>

        if (authRole != AuthenticationRole.Admin && authUser != username.trim) {
          throw UC4Exception.OwnerMismatch
        }

        val matriculationProposal = rawMatriculationProposal.trim

        var validationList = try {
          Await.result(matriculationProposal.validate, validationTimeout)
        }
        catch {
          case _: TimeoutException => throw UC4Exception.ValidationTimeout
          case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
        }

        examregService.getExaminationRegulations(Some(matriculationProposal.matriculation.map(_.fieldOfStudy).mkString(",")), Some(true)).invoke().flatMap {
          examRegs =>
            // Check for all subjects matriculations that the field of study is a valid examreg
            if (examRegs.isEmpty) {
              for (index <- matriculationProposal.matriculation.indices) {
                validationList :+= SimpleError(s"matriculation[$index]", "Field of Study is not an active examination regulation.")
              }
            }
            else {
              val examregList = examRegs.map(_.name)
              for (index <- matriculationProposal.matriculation.indices) {
                if (!examregList.contains(matriculationProposal.matriculation(index).fieldOfStudy)) {
                  validationList :+= SimpleError(s"matriculation[$index]", "Field of Study is not an active examination regulation.")
                }
              }
            }

            if (validationList.nonEmpty) {
              throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationList))
            }

            certificateService.getEnrollmentId(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
              .flatMap { jsonEnrollmentId =>
                val enrollmentId = jsonEnrollmentId.id

                certificateService.getCertificate(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke()
                  .flatMap { certificateJson =>
                    val certificate = certificateJson.certificate

                    entityRef.askWithStatus[ImmatriculationData](replyTo => GetMatriculationData(enrollmentId, replyTo))
                      .flatMap {
                        _ =>
                          entityRef.askWithStatus[ProposalWrapper](replyTo => GetProposalForAddEntriesToMatriculationData(
                            certificate,
                            enrollmentId,
                            matriculationProposal.matriculation,
                            replyTo
                          )).map {
                            proposalWrapper =>
                              operationService.addToWatchList(username).handleRequestHeader(addAuthenticationHeader(header)).invoke(JsonOperationId(proposalWrapper.operationId))
                                .recover {
                                  case ex: UC4Exception if ex.possibleErrorResponse.`type` == ErrorType.OwnerMismatch =>
                                  case throwable: Throwable =>
                                    log.error("Exception in addToWatchlist AddEntries", throwable)
                                }
                              (ResponseHeader(200, MessageProtocol.empty, List()), UnsignedProposal(proposalWrapper.proposal))
                          }.recover(handleException("Creation of add entry proposal failed"))

                      }.recoverWith {
                        case uc4Exception: UC4Exception if uc4Exception.errorCode == 404 =>
                          val data = ImmatriculationData(
                            enrollmentId,
                            matriculationProposal.matriculation
                          )
                          entityRef.askWithStatus[ProposalWrapper] { replyTo =>
                            GetProposalForAddMatriculationData(certificate, data, replyTo)
                          }.map {
                            proposalWrapper =>
                              operationService.addToWatchList(username).handleRequestHeader(addAuthenticationHeader(header)).invoke(JsonOperationId(proposalWrapper.operationId))
                                .recover {
                                  case ex: UC4Exception if ex.possibleErrorResponse.`type` == ErrorType.OwnerMismatch =>
                                  case throwable: Throwable =>
                                    log.error("Exception in addToWatchlist AddMatriculationData", throwable)
                                }
                              (ResponseHeader(200, MessageProtocol.empty, List()), UnsignedProposal(proposalWrapper.proposal))
                          }.recover(handleException("Creation of add matriculation data proposal failed"))

                        case uc4Exception: UC4Exception => throw uc4Exception

                        case ex: Throwable =>
                          throw UC4Exception.InternalServerError("Failure at addition of new matriculation data", ex.getMessage, ex)
                      }
                  }
              }
        }
      }
    }

  /** Returns the ImmatriculationData of a student with the given username */
  override def getMatriculationData(username: String): ServiceCall[NotUsed, ImmatriculationData] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Student) {
      (authUsername, role) =>
        ServerServiceCall { (header, _) =>
          if (role != AuthenticationRole.Admin && authUsername != username) {
            throw UC4Exception.OwnerMismatch
          }
          userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { user =>
            if (!user.isInstanceOf[Student]) {
              //We found a user, but it is not a Student. Therefore, a student with the username does not exist: NotFound
              throw UC4Exception.NotFound
            }
            certificateService.getEnrollmentId(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
              .flatMap { jsonEnrollmentId =>
                val enrollmentId = jsonEnrollmentId.id
                entityRef.askWithStatus[ImmatriculationData](replyTo => GetMatriculationData(enrollmentId, replyTo)).map {
                  data => createETagHeader(header, data)
                }.recover(handleException("get matriculation data failed"))
              }
          }
        }
    }

  /** Returns a pdf with the certificate of enrollment */
  override def getCertificateOfEnrollment(username: String, semester: String): ServiceCall[NotUsed, ByteString] =
    identifiedAuthenticated(AuthenticationRole.Student) {
      (authUsername, _) =>
        ServerServiceCall { (header, _) =>
          if (authUsername != username) {
            throw UC4Exception.OwnerMismatch
          }
          var pdf = certificateEnrollmentHtml

          userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { user =>
            getMatriculationData(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { data =>

              pdf = pdf.replace("{studentName}", s"${user.firstName}  ${user.lastName}")
              pdf = pdf.replace("{matriculationId}", user.asInstanceOf[Student].matriculationId)
              pdf = pdf.replace("{enrollmentId}", data.enrollmentId)
              pdf = pdf.replace("{semester}", semester)
              pdf = pdf.replace("{startDate}", Utils.semesterToStartDate(semester))
              pdf = pdf.replace("{endDate}", Utils.semesterToEndDate(semester))
              pdf = pdf.replace("{address}", config.getString("uc4.pdf.address").replace("\n", "<br>"))
              pdf = pdf.replace("{organization}", config.getString("uc4.pf.organization"))
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
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = ServiceCall { _ =>
    HyperledgerUtils.VersionUtil.createHyperledgerVersionResponse(entityRef)
  }
}
