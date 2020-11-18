package de.upb.cs.uc4.examreg.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }
import de.upb.cs.uc4.shared.client.UC4Service
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The ExamregService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the ExamregService.
  */
trait ExamregService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix = "/examreg-management"
  override val name = "examreg"

  /** Get all examination regulations, or the ones specified by the query parameter */
  def getExaminationRegulations(regulations: Option[String], active: Option[Boolean]): ServiceCall[NotUsed, Seq[ExaminationRegulation]]

  /** Get all names of examination regulations */
  def getExaminationRegulationsNames(active: Option[Boolean]): ServiceCall[NotUsed, Seq[String]]

  /** Get modules from all examination regulations, optionally filtered by Ids*/
  def getModules(moduleIds: Option[String], active: Option[Boolean]): ServiceCall[NotUsed, Seq[Module]]

  /** Add an examination regulation */
  def addExaminationRegulation(): ServiceCall[ExaminationRegulation, ExaminationRegulation]

  /** Set an examination regulation to inactive */
  def closeExaminationRegulation(examregName: String): ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedMethodsGET: ServiceCall[NotUsed, Done]

  /** Allows GET and POST */
  def allowedMethodsGETPOST: ServiceCall[NotUsed, Done]

  /** Allows DELETE */
  def allowedMethodsDELETE: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/examination-regulations?regulations&active", getExaminationRegulations _),
        restCall(Method.POST, pathPrefix + "/examination-regulations", addExaminationRegulation _)
        (CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.jsValueFormatMessageSerializer),

        restCall(Method.GET, pathPrefix + "/examination-regulations/modules?moduleIds&active", getModules _),
        restCall(Method.GET, pathPrefix + "/examination-regulations/names?active", getExaminationRegulationsNames _),
        restCall(Method.DELETE, pathPrefix + "/examination-regulations/:examregName", closeExaminationRegulation _),

        restCall(Method.OPTIONS, pathPrefix + "/examination-regulations", allowedMethodsGETPOST _),
        restCall(Method.OPTIONS, pathPrefix + "/examination-regulations/modules", allowedMethodsGET _),
        restCall(Method.OPTIONS, pathPrefix + "/examination-regulations/names", allowedMethodsGET _),
        restCall(Method.OPTIONS, pathPrefix + "/examination-regulations/:examregName", allowedMethodsDELETE _)
      )
  }
}
