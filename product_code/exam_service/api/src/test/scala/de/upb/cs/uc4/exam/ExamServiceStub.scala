package de.upb.cs.uc4.exam

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, UnsignedProposal }
import de.upb.cs.uc4.shared.client.JsonUtility.ToJsonUtil

import scala.concurrent.Future

class ExamServiceStub() extends ExamService with DefaultTestExams {

  protected var exams: Seq[Exam] = Seq()

  /** Delete all exams */
  def reset(): Unit = {
    exams = Seq()
  }

  def addExam(exam: Exam): Unit = {
    exams :+= exam
  }

  def setup(): Unit = {
    exams ++= Seq(exam0, exam1, exam2, exam3)

  }

  /** Returns Exams, optionally filtered */
  override def getExams(examIds: Option[String], courseIds: Option[String], lecturerIds: Option[String], moduleIds: Option[String], types: Option[String], admittableAt: Option[String], droppableAt: Option[String]): ServiceCall[NotUsed, Seq[Exam]] = {
    _ => Future.successful(exams.filter(exam => examIds.isEmpty || examIds.get.contains(exam.examId)))
  }

  /** Get a proposal for adding an Exam */
  override def getProposalAddExam: ServiceCall[Exam, UnsignedProposal] =
    Exam => Future.successful(UnsignedProposal(Exam.toJson.getBytes))

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall {
    _ => Future.successful(Done)
  }

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = ServiceCall {
    _ => Future.successful(Done)
  }

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = ServiceCall {
    _ => Future.successful(JsonHyperledgerVersion("undefined", "undefined"))
  }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall {
    _ => Future.successful(Done)
  }
}
