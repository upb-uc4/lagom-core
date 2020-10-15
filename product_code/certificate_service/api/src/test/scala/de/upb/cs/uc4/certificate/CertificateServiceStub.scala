package de.upb.cs.uc4.certificate

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.model.{ EncryptedPrivateKey, JsonCertificate, JsonEnrollmentId, PostMessageCSR }
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception

import scala.collection.mutable
import scala.concurrent.Future

class CertificateServiceStub extends CertificateService {
  private val certificateUsers = mutable.HashMap[String, CertificateUserEntry]()

  /** Create CertficateUser data for the given usernames. Without futures, for your convenience.
    *
    */
  def setup(usernames: String*): Unit = {
    usernames.foreach { username =>
      certificateUsers.put(username, CertificateUserEntry(username + "enrollmentId", username + "enrollmentSecret", username + "certificate", EncryptedPrivateKey("", "", "")))
    }
  }

  /** Fetch CertficateUser data for the given username. Without futures, for your convenience.
    *
    */
  def get(username: String): CertificateUserEntry = {
    certificateUsers.get(username) match {
      case Some(certificateUserEntry) => certificateUserEntry
      case None => throw UC4Exception.NotFound
    }
  }

  /** Forwards the certificate signing request from the given user */
  override def setCertificate(username: String): ServiceCall[PostMessageCSR, Done] = ServiceCall {
    postMessageCSR =>
      certificateUsers.put(username, CertificateUserEntry(username + "enrollmentId", username + "enrollmentSecret", username + "certificate", postMessageCSR.encryptedPrivateKey))
      Future.successful(Done)
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
  override def getEnrollmentId(username: String): ServiceCall[NotUsed, JsonEnrollmentId] = ServiceCall {
    _ =>
      certificateUsers.get(username) match {
        case Some(certificateUserEntry) => Future.successful(JsonEnrollmentId(certificateUserEntry.enrollmentId))
        case None => Future.failed(UC4Exception.NotFound)
      }
  }

  /** Returns the encrypted private key of the given user */
  override def getPrivateKey(username: String): ServiceCall[NotUsed, EncryptedPrivateKey] = ServiceCall {
    _ =>
      certificateUsers.get(username) match {
        case Some(certificateUserEntry) => Future.successful(certificateUserEntry.encryptedPrivateKey)
        case None => Future.failed(UC4Exception.NotFound)
      }
  }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }
}
