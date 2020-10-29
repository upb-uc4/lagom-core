package de.upb.cs.uc4.examreg

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }

import scala.concurrent.{ ExecutionContext, Future }

class ExamregServiceStub extends ExamregService with DefaultTestExamRegs {

  val examRegs = Seq(examReg0, examReg1)

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

  /** Allows GET */
  override def allowedMethodsGET: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }
}
