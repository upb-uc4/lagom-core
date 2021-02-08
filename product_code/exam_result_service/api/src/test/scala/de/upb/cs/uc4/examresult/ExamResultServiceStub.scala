package de.upb.cs.uc4.examresult

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.examresult.api.ExamResultService
import de.upb.cs.uc4.examresult.model.{ ExamResult, ExamResultEntry }
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, UnsignedProposal }

import scala.concurrent.Future

class ExamResultServiceStub extends ExamResultService {

  private var examResults = Seq[ExamResultEntry]()

  /** Clears all entries and adds the given entries to the store */
  def setup(examResultEntry: ExamResultEntry*): Unit = {
    clear()
    addAll(examResultEntry: _*)
  }

  /** Adds all examResultsEntries to the store */
  def addAll(examResultEntry: ExamResultEntry*): Unit = {
    examResults ++= examResultEntry
  }

  /** Clears all examResultEntries */
  def clear(): Unit = {
    examResults = Seq()
  }

  /** Returns ExamResultEntries, optionally filtered */
  override def getExamResults(username: Option[String], examIds: Option[String]): ServiceCall[NotUsed, Seq[ExamResultEntry]] = {
    _ =>
      Future.successful(examResults
        .filter(entry => username.isEmpty || entry.enrollmentId == username.get)
        .filter(entry => examIds.isEmpty || examIds.get.split(",").toSeq.contains(entry.examId)))
  }

  /** Get a proposals for adding the result of an exam */
  override def getProposalAddExamResult: ServiceCall[ExamResult, UnsignedProposal] = {
    _ => Future.successful(UnsignedProposal(""))
  }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = {
    _ => Future.successful(JsonHyperledgerVersion("", ""))
  }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)
}
