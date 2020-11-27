package de.upb.cs.uc4.admission.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.admission.model.{ CourseAdmission, DropAdmission }
import de.upb.cs.uc4.shared.client.{ SignedTransactionProposal, TransactionProposal, UC4Service }
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The AdmissionService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the AdmissionService.
  */
trait AdmissionService extends UC4Service {
  override val pathPrefix: String = "/admission-management"
  override val name: String = "admission"
  override val autoAcl: Boolean = true

  /** Returns course admissions */
  def getCourseAdmissions(username: Option[String], courseId: Option[String], moduleId: Option[String]): ServiceCall[NotUsed, Seq[CourseAdmission]]

  /** Gets a proposal for adding a course admission */
  def getProposalAddCourseAdmission: ServiceCall[CourseAdmission, TransactionProposal]

  /** Gets a proposal for dropping a course admission */
  def getProposalDropCourseAdmission: ServiceCall[DropAdmission, TransactionProposal]

  /** Submits a proposal */
  def submitProposal(): ServiceCall[SignedTransactionProposal, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows POST */
  def allowedPost: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/admissions/courses?username&courseId&moduleId", getCourseAdmissions _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.POST, pathPrefix + "/admissions/courses/proposals/add", getProposalAddCourseAdmission _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.POST, pathPrefix + "/admissions/courses/proposals/drop", getProposalDropCourseAdmission _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.POST, pathPrefix + "/admissions/proposals/submit", submitProposal _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),

        restCall(Method.OPTIONS, pathPrefix + "/admissions/courses?username&courseId&moduleId", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/courses/proposals/add", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/courses/proposals/drop", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/proposals/submit", allowedPost _),
      )
  }
}
