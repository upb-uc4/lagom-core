package de.upb.cs.uc4.examresult.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.upb.cs.uc4.examresult.model.{ExamResult, ExamResultEntry}
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer
import de.upb.cs.uc4.shared.client.{UC4HyperledgerService, UnsignedProposal}

trait ExamResultService extends UC4HyperledgerService {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix: String = "/exam-result-management"
  /** The name of the service */
  override val name: String = "exam-result"

  /** Returns ExamResultEntries, optionally filtered */
  def getExamResults(username: Option[String], examIds: Option[String]): ServiceCall[NotUsed, Seq[ExamResultEntry]]

  /** Get a proposals for adding the result of an exam */
  def getProposalAddExamResult(): ServiceCall[ExamResult, UnsignedProposal]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows POST */
  def allowedPost: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/exam_results?username&examIds", getExamResults _)(MessageSerializer.NotUsedMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/exam_results?username&examIds", allowedGet _),

        restCall(Method.POST, pathPrefix + "/exam_results/unsigned_add_proposal", getProposalAddExamResult _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/exam_results/unsigned_add_proposal", allowedPost _),
      )
  }
}
