package de.upb.cs.uc4.certificate.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.certificate.model.{ EncryptedPrivateKey, JsonCertificate, JsonEnrollmentId, PostMessageCSR }
import de.upb.cs.uc4.shared.client.UC4Service
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The CertificateService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the CertificateService.
  */
trait CertificateService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix = "/certificate-management"
  /** The name of the service */
  override val name = "certificate"

  /** Forwards the certificate signing request from the given user */
  def setCertificate(username: String): ServiceCall[PostMessageCSR, Done]

  /** Returns the certificate of the given user */
  def getCertificate(username: String): ServiceCall[NotUsed, JsonCertificate]

  /** Returns the enrollment id of the given user */
  def getEnrollmentId(username: String): ServiceCall[NotUsed, JsonEnrollmentId]

  /** Returns the encrypted private key of the given user */
  def getPrivateKey(username: String): ServiceCall[NotUsed, EncryptedPrivateKey]

  // OPTIONS
  /** Allows POST */
  def allowedPost: ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.POST, pathPrefix + "/certificates/:username", setCertificate _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.GET, pathPrefix + "/certificates/:username/certificate", getCertificate _),
        restCall(Method.GET, pathPrefix + "/certificates/:username/enrollmentId", getEnrollmentId _),
        restCall(Method.GET, pathPrefix + "/certificates/:username/privateKey", getPrivateKey _),
        restCall(Method.OPTIONS, pathPrefix + "/certificates/:username", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/certificates/:username/certificate", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/certificates/:username/enrollmentId", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/certificates/:username/privateKey", allowedGet _)
      )
      .withAutoAcl(true)
  }
}