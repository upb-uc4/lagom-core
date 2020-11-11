package de.upb.cs.uc4.examreg.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.typesafe.config.Config
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.actor.{ ExamregHyperledgerBehaviour, ExamregState }
import de.upb.cs.uc4.examreg.impl.commands.{ ExamregCommand, GetExamreg }
import de.upb.cs.uc4.examreg.impl.readside.{ ExamregDatabase, ExamregEventProcessor }
import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }
import de.upb.cs.uc4.hyperledger.commands.HyperledgerCommand
import de.upb.cs.uc4.shared.server.ServiceCallFactory._

import scala.concurrent.{ ExecutionContext, Future }

/** Implementation of the ExamregService */
class ExamregServiceImpl(clusterSharding: ClusterSharding, readSide: ReadSide,
    processor: ExamregEventProcessor, database: ExamregDatabase)(implicit ec: ExecutionContext, config: Config, timeout: Timeout) extends ExamregService {
  readSide.register(processor)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[ExamregCommand] =
    clusterSharding.entityRefFor(ExamregState.typeKey, id)

  /** Returns the entity for Hyperledger */
  private def entityRefHyperledger: EntityRef[HyperledgerCommand] =
    clusterSharding.entityRefFor(ExamregHyperledgerBehaviour.typeKey, ExamregHyperledgerBehaviour.entityId)

  // TODO All ExamReg Tests
  // TODO Default ExamRegs

  /** @inheritdoc */
  override def getExaminationRegulations(regulations: Option[String], active: Option[Boolean]): ServiceCall[NotUsed, Seq[ExaminationRegulation]] =
    ServiceCall { _ =>
      database.getAll
        .map(names => names
          .map(entityRef(_).ask[Option[ExaminationRegulation]](replyTo => GetExamreg(replyTo))))
        .flatMap{seq => Future.sequence(seq)
          .map{seq => seq
            .filter(opt => opt.isDefined) //Filter every not existing examination regulation
            .map(opt => opt.get)
            .filter(examReg => regulations.isEmpty || regulations.get.split(",").contains(examReg.name))
            .filter(examReg => active.isEmpty || active.get == examReg.active)
          }
        }
    }

  /** @inheritdoc */
  override def getModules(moduleIds: Option[String], active: Option[Boolean]): ServiceCall[NotUsed, Seq[Module]] = ServiceCall {
    _ =>
      getExaminationRegulations(None, None).invoke().map {
        _.filter(active.isEmpty || _.active == active.get)
          .flatMap(x => x.modules).distinct
          .filter(module => moduleIds.isEmpty || moduleIds.contains(module.id))
      }
  }

  /** @inheritdoc */
  override def getExaminationRegulationsNames(active: Option[Boolean]): ServiceCall[NotUsed, Seq[String]] = ServiceCall { _ =>
    active match {
      case None => database.getAll
      case Some(isActive) => getExaminationRegulations(None, None).invoke().map {
        _.filter(_.active == isActive).map(_.name)
      }
    }

  }

  /** @inheritdoc */
  override def allowedMethodsGET: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

}
