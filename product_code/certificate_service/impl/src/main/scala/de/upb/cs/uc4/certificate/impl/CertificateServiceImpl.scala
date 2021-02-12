package de.upb.cs.uc4.certificate.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{ EventStreamElement, PersistentEntityRegistry }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.impl.actor.{ CertificateState, CertificateUser }
import de.upb.cs.uc4.certificate.impl.commands.{ CertificateCommand, GetCertificateUser, SetCertificateAndKey }
import de.upb.cs.uc4.certificate.impl.events.{ CertificateEvent, OnCertficateAndKeySet }
import de.upb.cs.uc4.certificate.impl.readside.CertificateDatabase
import de.upb.cs.uc4.certificate.model._
import de.upb.cs.uc4.hyperledger.api.model.JsonHyperledgerVersion
import de.upb.cs.uc4.hyperledger.impl.HyperledgerUtils.ExceptionUtils
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerAdminParts, HyperledgerUtils }
import de.upb.cs.uc4.hyperledger.utilities.traits.EnrollmentManagerTrait
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionUtility
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/** Implementation of the UserService */
class CertificateServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    enrollmentManager: EnrollmentManagerTrait,
    kafkaEncryptionUtility: KafkaEncryptionUtility,
    database: CertificateDatabase,
    override val environment: Environment
)(implicit ec: ExecutionContext, timeout: Timeout, val config: Config)
  extends CertificateService with HyperledgerAdminParts {

  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[CertificateCommand] =
    clusterSharding.entityRefFor(CertificateState.typeKey, id)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  /** Forwards the certificate signing request from the given user */
  override def setCertificate(username: String): ServerServiceCall[PostMessageCSR, JsonCertificate] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUsername, role) =>
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
          throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
        }

        getCertificateUser(username).flatMap {
          case CertificateUser(Some(enrollmentId), Some(enrollmentSecret), None, None) =>
            try {
              val certificate = enrollmentManager.enrollSecure(caURL, tlsCert, enrollmentId, enrollmentSecret, pmcsrRaw.certificateSigningRequest, adminUsername, walletPath, channel, chaincode, networkDescriptionPath)
              entityRef(username).ask[Confirmation](replyTo => SetCertificateAndKey(role.toString, certificate, pmcsrRaw.encryptedPrivateKey, replyTo)).map {
                case Accepted(_) =>
                  val header = ResponseHeader(201, MessageProtocol.empty, List())
                    .addHeader("Location", s"$pathPrefix/certificates/$username/certificate")
                  (header, JsonCertificate(certificate))
                case Rejected(code, reason) =>
                  throw UC4Exception(code, reason)
                case _ =>
                  throw UC4Exception.InternalServerError("Unexpected Error", "Unexpected error occurred when fetching certificate")
              }
            }
            catch {
              case ex: Throwable => throw ex.toUC4Exception
            }
          case CertificateUser(_, _, Some(_), _) =>
            throw UC4Exception.AlreadyEnrolled
          case actorContent =>
            throw UC4Exception.InternalServerError("Failed to enroll user", s"Unexpected actor content: $actorContent")
        }
      }
  }

  /** Returns the certificate of the given user */
  override def getCertificate(username: String): ServiceCall[NotUsed, JsonCertificate] = authenticated(AuthenticationRole.All: _*) {
    ServerServiceCall { (header, _) =>
      getCertificateUser(username).map {
        case CertificateUser(_, _, Some(certificate), _) =>
          createETagHeader(header, JsonCertificate(certificate))
        case _ =>
          throw UC4Exception.NotFound
      }
    }
  }

  /** Returns the encrypted private key of the given user */
  override def getPrivateKey(username: String): ServiceCall[NotUsed, EncryptedPrivateKey] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUsername, _) =>
      ServerServiceCall { (header, _) =>

        if (authUsername != username) {
          throw UC4Exception.OwnerMismatch
        }

        getCertificateUser(username).map {
          case CertificateUser(_, _, _, Some(key)) =>
            createETagHeader(header, key)
          case _ =>
            throw UC4Exception.NotEnrolled
        }
      }
  }

  /** Returns the enrollment id of the given user */
  override def getEnrollmentIds(usernames: Option[String]): ServiceCall[NotUsed, Seq[UsernameEnrollmentIdPair]] = authenticated(AuthenticationRole.All: _*) {
    ServerServiceCall { (header, _) =>
      if (usernames.isEmpty) {
        throw UC4Exception.QueryParameterError(SimpleError("usernames", "Query parameter must be set"))
      }
      val usernameList = usernames.get.split(",").filter(_.trim.nonEmpty).toSeq

      Future.sequence(usernameList.map {
        username =>
          getCertificateUser(username).map {
            case CertificateUser(Some(id), _, _, _) =>
              UsernameEnrollmentIdPair(username, id)
            case _ =>
              UsernameEnrollmentIdPair("", "")
          }
      }).map(_.filter(_.username.trim.nonEmpty))
        .map(response => createETagHeader(header, response))
    }
  }

  /** Returns the username that matches the given enrollmentId */
  override def getUsernames(enrollmentIds: Option[String]): ServiceCall[NotUsed, Seq[UsernameEnrollmentIdPair]] = authenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
    ServerServiceCall {
      (header, _) =>
        if (enrollmentIds.isEmpty) {
          throw UC4Exception.QueryParameterError(SimpleError("enrollmentIds", "Query parameter must be set"))
        }
        val enrollmentIdList = enrollmentIds.get.split(",").filter(_.trim.nonEmpty).toSeq

        database.getUsernameEnrollmentIdPairs(enrollmentIdList)
          .map(usernameEnrollmentIdPairs => createETagHeader(header, usernameEnrollmentIdPairs))
    }
  }

  /** Publishes every user that is registered at hyperledger */
  override def userEnrollmentTopic(): Topic[EncryptionContainer] = TopicProducer.singleStreamWithOffset { fromOffset =>
    persistentEntityRegistry
      .eventStream(CertificateEvent.Tag, fromOffset)
      .mapConcat {
        //Filter only OnRegisterUser events
        case EventStreamElement(_, OnCertficateAndKeySet(enrollmentId, role, _, _), offset) => try {
          immutable.Seq((kafkaEncryptionUtility.encrypt(EnrollmentUser(enrollmentId, role)), offset))
        }
        catch {
          case throwable: Throwable =>
            log.error("CertificateService cannot send invalid registration topic message {}", throwable.toString)
            Nil
        }
        case _ => Nil
      }
  }

  /** Helper method for getting the actor that corresponds to the given username */
  private def getCertificateUser(username: String): Future[CertificateUser] =
    entityRef(username).ask[CertificateUser](replyTo => GetCertificateUser(replyTo))

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")

  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = ServiceCall { _ =>
    HyperledgerUtils.VersionUtil.createHyperledgerAPIVersionResponse()
  }
}
