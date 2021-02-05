package de.upb.cs.uc4.matriculation.api

import akka.util.ByteString
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.hyperledger.api.UC4HyperledgerService
import de.upb.cs.uc4.hyperledger.api.model.UnsignedProposal
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation }
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The MatriculationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the MatriculationService.
  */
trait MatriculationService extends UC4HyperledgerService {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  override val pathPrefix: String = "/matriculation-management"
  /** The name of the service */
  override val name: String = "matriculation"

  /** Get proposal to matriculate a student */
  def getMatriculationProposal(username: String): ServiceCall[PutMessageMatriculation, UnsignedProposal]

  /** Returns the ImmatriculationData of a student with the given username */
  def getMatriculationData(username: String): ServiceCall[NotUsed, ImmatriculationData]

  /** Returns a pdf with the certificate of enrollment */
  def getCertificateOfEnrollment(username: String): ServiceCall[NotUsed, ByteString]

  /** Allows POST */
  def allowedPost: ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.POST, pathPrefix + "/matriculation/:username/unsigned_proposal", getMatriculationProposal _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.GET, pathPrefix + "/matriculation/:username", getMatriculationData _),
        restCall(Method.GET, pathPrefix + "/pdf/:username", getCertificateOfEnrollment _),

        restCall(Method.OPTIONS, pathPrefix + "/matriculation/:username/unsigned_proposal", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/matriculation/:username", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/pdf/:username", allowedGet _)
      )
  }
}
