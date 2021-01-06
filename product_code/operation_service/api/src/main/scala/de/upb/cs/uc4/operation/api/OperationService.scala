package de.upb.cs.uc4.operation.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.operation.model.OperationData
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The OperationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the OperationService.
  */
trait OperationService extends UC4HyperledgerService {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  override val pathPrefix: String = "/operation-management"
  /** The name of the service */
  override val name: String = "operation"

  /** Returns the Operations for the matching operationId */
  def getOperations(operationId: String): ServiceCall[NotUsed, OperationData]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.POST, pathPrefix + "/operation/:operationId", getOperations _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),

        restCall(Method.OPTIONS, pathPrefix + "/operation/:operationId", allowedGet _),
      )
  }
}
