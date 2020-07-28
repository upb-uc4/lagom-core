package de.upb.cs.uc4.matriculation.api

import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}
import de.upb.cs.uc4.shared.client.CustomExceptionSerializer
import play.api.Environment

/** The MatriculationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the MatriculationService.
  */
trait MatriculationService extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  val pathPrefix = "/matriculation-management"

  final override def descriptor: Descriptor = {
    import Service._
    named("matriculation")
      .withCalls(
        //restCall(Method.POST, pathPrefix + "/read/:transactionId", read _),
        //restCall(Method.POST, pathPrefix + "/write/:transactionId", write _),
      ).withExceptionSerializer(new CustomExceptionSerializer(Environment.simple()))
  }
}