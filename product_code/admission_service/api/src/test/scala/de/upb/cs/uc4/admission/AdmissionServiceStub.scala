package de.upb.cs.uc4.admission

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.admission.model.{ CourseAdmission, DropAdmission }
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, SignedProposal, SignedTransaction, UnsignedProposal, UnsignedTransaction }

import scala.concurrent.Future

class AdmissionServiceStub extends AdmissionService {

  protected var admissions: Seq[CourseAdmission] = Seq()

  def reset(): Unit = {
    admissions = Seq()
  }

  def add(courseAdmission: CourseAdmission): Unit = {
    admissions :+= courseAdmission
  }

  /** Returns course admissions */
  override def getCourseAdmissions(username: Option[String], courseId: Option[String], moduleId: Option[String]): ServiceCall[NotUsed, Seq[CourseAdmission]] = ServiceCall {
    _ => Future.successful(admissions)
  }

  /** Gets a proposal for adding a course admission */
  override def getProposalAddCourseAdmission: ServiceCall[CourseAdmission, UnsignedProposal] = ServiceCall {
    _ => Future.successful(UnsignedProposal(""))
  }

  /** Gets a proposal for dropping a course admission */
  override def getProposalDropCourseAdmission: ServiceCall[DropAdmission, UnsignedProposal] = ServiceCall {
    _ => Future.successful(UnsignedProposal(""))
  }

  /** Submits a proposal */
  override def submitProposal(): ServiceCall[SignedProposal, UnsignedTransaction] = ServiceCall {
    _ => Future.successful(UnsignedTransaction(""))
  }

  /** Submits a transaction */
  override def submitTransaction(): ServiceCall[SignedTransaction, Done] = ServiceCall {
    _ => Future.successful(Done)
  }

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
