package de.upb.cs.uc4.matriculation.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
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

  /** Immatriculates a student */
  def immatriculateStudent(): ServiceCall[ImmatriculationData, Done]

  /** Returns the ImmatriculationData of a student with the given matriculationId */
  def getMatriculation(matriculationId: String): ServiceCall[NotUsed, ImmatriculationData]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    import com.lightbend.lagom.scaladsl.api.ServiceAcl._
    named("matriculation")
      .withCalls(
        restCall(Method.POST, pathPrefix + "/immatriculation", immatriculateStudent _),
        restCall(Method.GET, pathPrefix + "/immatriculation/:id", getMatriculation _),
        restCall(Method.OPTIONS, pathPrefix + "/immatriculation/:id", allowedGet _),
      )
      .withAcls(
        forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/immatriculation/\\E" + "[0-9]{1,7}"),
        forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/immatriculation/\\E" + "[0-9]{1,7}"),
      )
      .withExceptionSerializer(new CustomExceptionSerializer(Environment.simple()))
  }
}