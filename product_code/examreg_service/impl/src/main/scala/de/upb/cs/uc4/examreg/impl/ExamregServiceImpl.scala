package de.upb.cs.uc4.examreg.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.typesafe.config.Config
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.actor.ExamregState
import de.upb.cs.uc4.examreg.impl.commands.{ ExamregCommand, GetExamreg }
import de.upb.cs.uc4.examreg.impl.readside.{ ExamregDatabase, ExamregEventProcessor }
import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, JsonExamRegNameList, JsonExaminationRegulations }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

/** Implementation of the ExamregService */
class ExamregServiceImpl(clusterSharding: ClusterSharding, readSide: ReadSide,
    processor: ExamregEventProcessor, database: ExamregDatabase)(implicit ec: ExecutionContext, config: Config) extends ExamregService {
  readSide.register(processor)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[ExamregCommand] =
    clusterSharding.entityRefFor(ExamregState.typeKey, id)

  implicit val timeout: Timeout = Timeout(15.seconds)

  /** @inheritdoc */
  override def getExaminationRegulations(regulations: Option[String]): ServiceCall[NotUsed, JsonExaminationRegulations] =
    ServiceCall { _ =>
      database.getAll
        .map(names => names
          .map(entityRef(_).ask[Option[ExaminationRegulation]](replyTo => GetExamreg(replyTo))))
        .flatMap(seq => Future.sequence(seq)
          .map(seq => seq
            .filter(opt => opt.isDefined) //Filter every not existing examination regulation
            .map(opt => opt.get)))
        .map(examregs => JsonExaminationRegulations(examregs))
    }

  /** @inheritdoc */
  override def getExaminationRegulationsNames: ServiceCall[NotUsed, JsonExamRegNameList] = ServiceCall { _ =>
    database.getAll.map(seq => JsonExamRegNameList(seq))
  }

  /** @inheritdoc */
  override def allowedMethodsGET: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
