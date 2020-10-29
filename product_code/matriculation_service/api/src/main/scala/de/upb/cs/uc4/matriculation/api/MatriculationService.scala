package de.upb.cs.uc4.matriculation.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation }
import de.upb.cs.uc4.shared.client.{ SignedTransactionProposal, TransactionProposal, UC4Service }
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The MatriculationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the MatriculationService.
  */
trait MatriculationService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  override val pathPrefix: String = "/matriculation-management"
  /** The name of the service */
  override val name: String = "matriculation"

  /** Immatriculates a student */
  def addMatriculationData(username: String): ServiceCall[SignedTransactionProposal, Done]

  /** Get proposal to immatriculates a student */
  def getMatriculationDataProposal(username: String): ServiceCall[PutMessageMatriculation, TransactionProposal]

  /** Returns the ImmatriculationData of a student with the given username */
  def getMatriculationData(username: String): ServiceCall[NotUsed, ImmatriculationData]

  /** Allows POST */
  def allowedPost: ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.POST, pathPrefix + "/matriculation/:username/submit", addMatriculationData _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.POST, pathPrefix + "/matriculation/:username/proposal", getMatriculationDataProposal _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.GET, pathPrefix + "/history/:username", getMatriculationData _),
        restCall(Method.OPTIONS, pathPrefix + "/matriculation/:username/submit", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/matriculation/:username/proposal", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/history/:username", allowedGet _),
      )
  }
}
