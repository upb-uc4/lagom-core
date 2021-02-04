package de.upb.cs.uc4.exam.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.hyperledger.api.UC4HyperledgerService
import de.upb.cs.uc4.hyperledger.api.model.UnsignedProposal
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer

/** The MatriculationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the MatriculationService.
  */
trait ExamService extends UC4HyperledgerService {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  override val pathPrefix: String = "/exam-management"
  /** The name of the service */
  override val name: String = "exam"

  /** Returns Exams, optionally filtered */
  def getExams(examIds: Option[String], courseIds: Option[String], lecturerIds: Option[String], moduleIds: Option[String], types: Option[String], admittableAt: Option[String], droppableAt: Option[String]): ServiceCall[NotUsed, Seq[Exam]]

  /** Get a proposal for adding an Exam */
  def getProposalAddExam: ServiceCall[Exam, UnsignedProposal]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows POST */
  def allowedPost: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/exams?examIds&courseIds&lecturerIds&moduleIds&types&admittableAt&droppableAt", getExams _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/exams?examIds&courseIds&lecturerIds&moduleIds&types&admittableAt&droppableAt", allowedGet _),

        restCall(Method.POST, pathPrefix + "/exams/unsigned_add_proposal", getProposalAddExam _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/exams/unsigned_add_proposal", allowedPost _),
      )
  }
}
