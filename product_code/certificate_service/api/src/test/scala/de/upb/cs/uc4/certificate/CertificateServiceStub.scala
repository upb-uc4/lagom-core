package de.upb.cs.uc4.certificate

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.model.{ EncryptedPrivateKey, JsonCertificate, PostMessageCSR, UsernameEnrollmentIdPair }
import de.upb.cs.uc4.hyperledger.api.model.JsonHyperledgerVersion
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer

import scala.collection.mutable
import scala.concurrent.Future

class CertificateServiceStub extends CertificateService {
  private val certificateUsers = mutable.HashMap[String, CertificateUserEntry]()

  /** Create CertficateUser data for the given usernames. Without futures, for your convenience. */
  def addAll(usernames: String*): Unit = {
    usernames.foreach { username =>
      certificateUsers.put(username, CertificateUserEntry(username + "enrollmentId", username + "enrollmentSecret", username + "certificate", EncryptedPrivateKey("", "", "")))
    }
  }

  /** Deletes all certificate users */
  def reset(): Unit = {
    certificateUsers.clear()
  }

  /** Deletes all certificate users and adds the given users */
  def setup(usernames: String*): Unit = {
    reset()
    addAll(usernames: _*)
  }

  /** Fetch CertficateUser data for the given username. Without futures, for your convenience. */
  def get(username: String): CertificateUserEntry = {
    certificateUsers.get(username) match {
      case Some(certificateUserEntry) => certificateUserEntry
      case None => throw UC4Exception.NotFound
    }
  }

  /** Forwards the certificate signing request from the given user */
  override def setCertificate(username: String): ServiceCall[PostMessageCSR, JsonCertificate] = ServiceCall {
    postMessageCSR =>
      certificateUsers.put(username, CertificateUserEntry(username + "enrollmentId", username + "enrollmentSecret", username + "certificate", postMessageCSR.encryptedPrivateKey))
      Future.successful(JsonCertificate(username + "certificate"))
  }

  /** Returns the certificate of the given user */
  override def getCertificate(username: String): ServiceCall[NotUsed, JsonCertificate] = ServiceCall {
    _ =>
      certificateUsers.get(username) match {
        case Some(certificateUserEntry) => Future.successful(JsonCertificate(certificateUserEntry.certificate))
        case None => Future.failed(UC4Exception.NotFound)
      }
  }

  /** Returns the enrollment id of the given user */
  override def getEnrollmentIds(usernames: Option[String]): ServiceCall[NotUsed, Seq[UsernameEnrollmentIdPair]] = ServiceCall {
    _ =>
      val usernameList = usernames.get.split(",").filter(_.trim.nonEmpty).toSeq

      Future.successful(
        usernameList.map {
          username =>
            val enrollmentId = try {
              get(username).enrollmentId
            }
            catch {
              case uc4ex: UC4Exception if uc4ex.possibleErrorResponse.`type` == ErrorType.KeyNotFound => ""
              case ex: Exception => throw ex
            }
            UsernameEnrollmentIdPair(username, enrollmentId)
        }.filter(pair => pair.enrollmentId != "")
      )
  }

  /** Returns the encrypted private key of the given user */
  override def getPrivateKey(username: String): ServiceCall[NotUsed, EncryptedPrivateKey] = ServiceCall {
    _ =>
      certificateUsers.get(username) match {
        case Some(certificateUserEntry) => Future.successful(certificateUserEntry.encryptedPrivateKey)
        case None => Future.failed(UC4Exception.NotFound)
      }
  }

  /** Returns the username that matches the given enrollmentId */
  override def getUsernames(enrollmentIds: Option[String]): ServiceCall[NotUsed, Seq[UsernameEnrollmentIdPair]] = ServiceCall {
    _ =>
      val enrollmentIdList = enrollmentIds.get.split(",").filter(_.trim.nonEmpty).toSeq

      Future.successful(
        certificateUsers
          .filter(entry => enrollmentIdList.contains(entry._2.enrollmentId)).toSeq
          .map {
            case (username, certificateUserEntry) =>
              UsernameEnrollmentIdPair(username, certificateUserEntry.enrollmentId)
          }
      )

  }

  /** Publishes every user that is registered at hyperledger */
  override def userEnrollmentTopic(): Topic[EncryptionContainer] = null

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = { _ => Future.successful(JsonHyperledgerVersion("", "")) }
}
