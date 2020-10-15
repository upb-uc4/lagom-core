package de.upb.cs.uc4.certificate.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.impl.actor.CertificateState
import de.upb.cs.uc4.certificate.impl.commands.{ CertificateCommand, GetCertificateUser, SetCertificateAndKey }
import de.upb.cs.uc4.certificate.impl.readside.CertificateEventProcessor
import de.upb.cs.uc4.certificate.model.{ EncryptedPrivateKey, JsonCertificate, JsonEnrollmentId, PostMessageCSR }
import de.upb.cs.uc4.hyperledger.HyperledgerAdminParts
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.ExceptionUtils
import de.upb.cs.uc4.hyperledger.utilities.traits.EnrollmentManagerTrait
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, RejectedWithError }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/** Implementation of the UserService */
class CertificateServiceImpl(
    clusterSharding: ClusterSharding,
    enrollmentManager: EnrollmentManagerTrait,
    readSide: ReadSide, processor: CertificateEventProcessor
)(implicit ec: ExecutionContext, val config: Config)
  extends CertificateService with HyperledgerAdminParts {
  readSide.register(processor)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[CertificateCommand] =
    clusterSharding.entityRefFor(CertificateState.typeKey, id)

  implicit val timeout: Timeout = Timeout(15.seconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.validation.timeout").milliseconds

  /** Forwards the certificate signing request from the given user */
  override def setCertificate(username: String): ServerServiceCall[PostMessageCSR, JsonCertificate] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUsername, _) =>
      ServerServiceCall { (_, pmcsrRaw) =>
        // One can only set his own certificate
        if (authUsername != username) {
          throw UC4Exception.OwnerMismatch
        }

        // Validate the CSR
        val validationErrorsFuture = pmcsrRaw.validate
        val validationErrors = try {
          Await.result(validationErrorsFuture, validationTimeout)
        }
        catch {
          case _: TimeoutException => throw UC4Exception.ValidationTimeout
          case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
        }
        if (validationErrors.nonEmpty) {
          throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
        }

        getCertificateUser(username).flatMap {
          case (Some(enrollmentId), Some(enrollmentSecret), None, None) =>
            try {
              val certificate = enrollmentManager.enrollSecure(caURL, tlsCert, enrollmentId, enrollmentSecret, pmcsrRaw.certificateSigningRequest, adminUsername, walletPath, channel, chaincode, networkDescriptionPath)
              entityRef(username).ask[Confirmation](replyTo => SetCertificateAndKey(certificate, pmcsrRaw.encryptedPrivateKey, replyTo)).map {
                case Accepted =>
                  val header = ResponseHeader(201, MessageProtocol.empty, List())
                    .addHeader("Location", s"$pathPrefix/certificates/$username/certificate")
                  (header, JsonCertificate(certificate))
                case RejectedWithError(code, reason) =>
                  throw new UC4Exception(code, reason)
                case _ =>
                  throw UC4Exception.InternalServerError("Unexpected Error", "Unexpected error occurred when fetching certificate")
              }
            }
            catch {
              case ex: Throwable => throw ex.toUC4Exception
            }
          case (_, _, Some(_), _) =>
            throw UC4Exception.AlreadyEnrolled
          case _ =>
            throw UC4Exception.InternalServerError("Failed to enroll user", "Unexpected actor content, maybe registration failed")
        }
      }
  }

  /** Returns the certificate of the given user */
  override def getCertificate(username: String): ServiceCall[NotUsed, JsonCertificate] = authenticated(AuthenticationRole.All: _*) { _ =>
    getCertificateUser(username).map {
      case (_, _, Some(certificate), _) =>
        JsonCertificate(certificate)
      case _ =>
        throw UC4Exception.NotFound
    }
  }

  /** Returns the enrollment id of the given user */
  override def getEnrollmentId(username: String): ServiceCall[NotUsed, JsonEnrollmentId] = authenticated(AuthenticationRole.All: _*) { _ =>
    getCertificateUser(username).map {
      case (Some(id), _, _, _) =>
        JsonEnrollmentId(id)
      case _ =>
        throw UC4Exception.NotFound
    }
  }

  /** Returns the encrypted private key of the given user */
  override def getPrivateKey(username: String): ServiceCall[NotUsed, EncryptedPrivateKey] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUsername, _) =>
      ServerServiceCall { (_, _) =>

        if (authUsername != username) {
          throw UC4Exception.OwnerMismatch
        }

        getCertificateUser(username).map {
          case (_, _, _, Some(key)) =>
            (ResponseHeader(200, MessageProtocol.empty, List()), key)
          case _ =>
            throw UC4Exception.NotEnrolled
        }
      }
  }

  /** Helper method for getting the actor that corresponds to the given username */
  private def getCertificateUser(username: String): Future[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])] =
    entityRef(username).ask[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])](replyTo => GetCertificateUser(replyTo))

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")

  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}