package de.upb.cs.uc4.examreg

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }
import de.upb.cs.uc4.shared.client.JsonHyperledgerVersion
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception

import scala.concurrent.Future

class ExamregServiceStub extends ExamregService with DefaultTestExamRegs {

  var examRegs = List(examReg0, examReg1)

  /** Get all examination regulations, or the ones specified by the query parameter */
  override def getExaminationRegulations(regulations: Option[String], active: Option[Boolean]): ServiceCall[NotUsed, Seq[ExaminationRegulation]] = ServiceCall {
    _ =>
      Future.successful(
        examRegs
          .filter(examinationRegulation => regulations.isEmpty || regulations.contains(examinationRegulation.name))
          .filter(active.isEmpty || _.active == active.get)
      )
  }

  /** Get all names of examination regulations */
  override def getExaminationRegulationsNames(active: Option[Boolean]): ServiceCall[NotUsed, Seq[String]] = ServiceCall {
    _ =>
      Future.successful(
        examRegs.
          filter(active.isEmpty || _.active == active.get).map(_.name)
      )
  }

  /** Get modules from all examination regulations, optionally filtered by Ids */
  override def getModules(moduleIds: Option[String], active: Option[Boolean]): ServiceCall[NotUsed, Seq[Module]] = ServiceCall {
    _ =>
      Future.successful(
        examRegs
          .filter(active.isEmpty || _.active == active.get).flatMap(x => x.modules).distinct
          .filter(module => moduleIds.isEmpty || moduleIds.get.split(",").contains(module.id))
      )
  }

  /** Add an examination regulation */
  override def addExaminationRegulation(): ServiceCall[ExaminationRegulation, ExaminationRegulation] = ServiceCall {
    examReg =>
      if (examRegs.map(_.name).contains(examReg.name)) {
        Future.failed(throw UC4Exception.Duplicate)
      }
      else {
        examRegs :+= examReg
        Future.successful(examReg)
      }
  }

  /** Set an examination regulation to inactive */
  override def closeExaminationRegulation(examregName: String): ServiceCall[NotUsed, Done] = ServiceCall { _ =>
    examRegs.find(_.name == examregName) match {
      case Some(examinationRegulation) =>
        examRegs = examRegs.filter(_.name != examregName)
        examRegs :+= examinationRegulation.copy(active = false)
        Future.successful(Done)
      case None =>
        Future.failed(UC4Exception.NotFound)
    }
  }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Allows GET */
  override def allowedMethodsGET: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Allows GET and POST */
  override def allowedMethodsGETPOST: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Allows DELETE */
  override def allowedMethodsDELETE: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = { _ => Future.successful(JsonHyperledgerVersion("", "")) }
}
