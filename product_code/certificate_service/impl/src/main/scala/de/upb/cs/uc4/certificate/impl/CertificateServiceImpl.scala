package de.upb.cs.uc4.certificate.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.impl.actor.CertificateState
import de.upb.cs.uc4.certificate.impl.commands.{ CertificateCommand, GetCertificateUser }
import de.upb.cs.uc4.certificate.model.{ EncryptedPrivateKey, JsonCertificate, JsonEnrollmentId, PostMessageCSR }
import de.upb.cs.uc4.hyperledger.HyperledgerAdminParts
import de.upb.cs.uc4.hyperledger.utilities.EnrollmentManager
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import de.upb.cs.uc4.shared.server.ServiceCallFactory._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** Implementation of the UserService */
class CertificateServiceImpl(clusterSharding: ClusterSharding)
                            (implicit ec: ExecutionContext, val config: Config)
  extends CertificateService with HyperledgerAdminParts {

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[CertificateCommand] =
    clusterSharding.entityRefFor(CertificateState.typeKey, id)

  implicit val timeout: Timeout = Timeout(15.seconds)

  /** Forwards the certificate signing request from the given user */
  override def setCertificate(username: String): ServerServiceCall[PostMessageCSR, Done] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUsername, role) =>
      ServerServiceCall { (_, pmcsrRaw) =>
        entityRef(username).ask[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])](replyTo => GetCertificateUser(replyTo))
          .map {
            case (Some(enrollmentId), Some(enrollmentSecret), _, _) =>
              EnrollmentManager.enroll(caURL, tlsCert, walletPath, enrollmentId, enrollmentSecret, organisationId)
              (ResponseHeader(202, MessageProtocol.empty, List()), Done)
            case _ =>
              throw CustomException.NotFound
          }
      }
  }

  /** Returns the certificate of the given user */
  override def getCertificate(username: String): ServiceCall[NotUsed, JsonCertificate] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUsername, role) =>
      ServerServiceCall { (_, _) =>
        entityRef(username).ask[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])](replyTo => GetCertificateUser(replyTo))
          .map {
            case (_, _, Some(certificate), _) =>
              (ResponseHeader(200, MessageProtocol.empty, List()), JsonCertificate(certificate))
            case _ =>
              throw CustomException.NotFound
          }
      }
  }

  /** Returns the enrollment id of the given user */
  override def getEnrollmentId(username: String): ServiceCall[NotUsed, JsonEnrollmentId] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUsername, role) =>
      ServerServiceCall { (_, _) =>
        entityRef(username).ask[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])](replyTo => GetCertificateUser(replyTo))
          .map {
            case (Some(id), _, _, _) =>
              (ResponseHeader(200, MessageProtocol.empty, List()), JsonEnrollmentId(id))
            case _ =>
              throw CustomException.NotFound
          }
      }
  }

  /** Returns the encrypted private key of the given user */
  override def getPrivateKey(username: String): ServiceCall[NotUsed, EncryptedPrivateKey] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUsername, role) =>
      ServerServiceCall { (_, _) =>
        entityRef(username).ask[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])](replyTo => GetCertificateUser(replyTo))
          .map {
            case (_, _, _, Some(key)) =>
              (ResponseHeader(200, MessageProtocol.empty, List()), key)
            case _ =>
              throw CustomException.NotFound
          }
      }
  }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}