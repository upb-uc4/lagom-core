package de.upb.cs.uc4.admission.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.admission.model.{ CourseAdmission, DropAdmission, ExamAdmission }
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer
import de.upb.cs.uc4.shared.client._

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

  /** Gets a proposal for adding a course admission */
  def getProposalAddCourseAdmission: ServiceCall[CourseAdmission, UnsignedProposal]

  /** Returns exam admissions */
  def getExamAdmissions(username: Option[String], admissionIDs: Option[String], examIDs: Option[String]): ServiceCall[NotUsed, Seq[ExamAdmission]]

  /** Gets a proposal for adding a exam admission */
  def getProposalAddExamAdmission: ServiceCall[ExamAdmission, UnsignedProposal]

  /** Gets a proposal for dropping a course admission */
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
        restCall(Method.POST, pathPrefix + "/admissions/courses/unsigned_add_proposal", getProposalAddCourseAdmission _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.GET, pathPrefix + "/admissions/exam?username&courseIds&examIds", getExamAdmissions _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.POST, pathPrefix + "/admissions/exam/unsigned_add_proposal", getProposalAddExamAdmission _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.POST, pathPrefix + "/admissions/courses/unsigned_drop_proposal", getProposalDropAdmission _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/courses?username&courseId&moduleId", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/courses/unsigned_add_proposal", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/exam?username&courseIds&examIds", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/exam/unsigned_add_proposal", allowedPost _),
        restCall(Method.OPTIONS, pathPrefix + "/admissions/unsigned_drop_proposal", allowedPost _)
      )
  }
}
