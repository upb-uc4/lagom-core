package de.upb.cs.uc4.examreg.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.examreg.model.{ JsonExamRegNameList, JsonExaminationRegulations }
import de.upb.cs.uc4.shared.client.UC4Service

/** The ExamregService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the ExamregService.
  */
trait ExamregService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix = "/exam-reg-management"
  override val name = "examreg"

  /** Get all examination regulations, or the ones specified by the query parameter */
  def getExaminationRegulations(regulations: Option[String]): ServiceCall[NotUsed, JsonExaminationRegulations]

  /** Get all names of examination regulations */
  def getExaminationRegulationsNames: ServiceCall[NotUsed, JsonExamRegNameList]

  /** Allows GET */
  def allowedMethodsGET: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/examination-regulations?regulations", getExaminationRegulations _),
        restCall(Method.GET, pathPrefix + "/examination-regulations/names", getExaminationRegulationsNames _),
        restCall(Method.OPTIONS, pathPrefix + "/examination-regulations", allowedMethodsGET _),
        restCall(Method.OPTIONS, pathPrefix + "/examination-regulations/names", allowedMethodsGET _),
      )
  }
}
