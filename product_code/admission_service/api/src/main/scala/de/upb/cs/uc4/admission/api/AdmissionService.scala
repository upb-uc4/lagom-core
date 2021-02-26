package de.upb.cs.uc4.admission.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.admission.model.{ AbstractAdmission, CourseAdmission, DropAdmission, ExamAdmission }
import de.upb.cs.uc4.hyperledger.api.UC4HyperledgerService
import de.upb.cs.uc4.hyperledger.api.model.UnsignedProposal
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The AdmissionService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the AdmissionService.
  */
trait AdmissionService extends UC4HyperledgerService {
  override val pathPrefix: String = "/admission-management"
  override val name: String = "admission"
  override val autoAcl: Boolean = true

  /** Returns course admissions */
  def getCourseAdmissions(username: Option[String], courseId: Option[String], moduleId: Option[String]): ServiceCall[NotUsed, Seq[CourseAdmission]]

  /** Returns exam admissions */
  def getExamAdmissions(username: Option[String], admissionIds: Option[String], examIds: Option[String]): ServiceCall[NotUsed, Seq[ExamAdmission]]

  /** Gets a proposal for adding a exam admission */
  def getProposalAddAdmission: ServiceCall[AbstractAdmission, UnsignedProposal]

  /** Gets a proposal for dropping an admission */
  def getProposalDropAdmission: ServiceCall[DropAdmission, UnsignedProposal]
  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows POST */
  def allowedPost: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/admissions/courses?username&courseId&moduleId", getCourseAdmissions _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.GET, pathPrefix + "/admissions/exams?username&admissionsIds&examIds", getExamAdmissions _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),

        restCall(Method.POST, pathPrefix + "/admissions/unsigned_add_proposal", getProposalAddAdmission _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.POST, pathPrefix + "/admissions/unsigned_drop_proposal", getProposalDropAdmission _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),

        restCall(Method.OPTIONS, pathPrefix + "/admissions/courses?username&courseId&moduleId", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/exams?username&admissionsIds&examIds", allowedGet _),

        restCall(Method.OPTIONS, pathPrefix + "/admissions/unsigned_add_proposal", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/unsigned_drop_proposal", allowedPost _)
      )
  }
}
