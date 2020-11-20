package de.upb.cs.uc4.examreg.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.actor.{ ExamregHyperledgerBehaviour, ExamregState }
import de.upb.cs.uc4.examreg.impl.commands._
import de.upb.cs.uc4.examreg.impl.readside.ExamregDatabase
import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }
import de.upb.cs.uc4.hyperledger.commands.HyperledgerBaseCommand
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, SimpleError, UC4Exception }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/** Implementation of the ExamregService */
class ExamregServiceImpl(clusterSharding: ClusterSharding, database: ExamregDatabase)(implicit ec: ExecutionContext, config: Config, timeout: Timeout) extends ExamregService {

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[ExamregCommand] =
    clusterSharding.entityRefFor(ExamregState.typeKey, id)

  /** Returns the entity for Hyperledger */
  private def entityRefHyperledger: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(ExamregHyperledgerBehaviour.typeKey, ExamregHyperledgerBehaviour.entityId)

  /** @inheritdoc */
  override def getExaminationRegulations(regulations: Option[String], active: Option[Boolean]): ServiceCall[NotUsed, Seq[ExaminationRegulation]] =
    ServiceCall { _ =>
      database.getAll
        .map(names => names
          .map(entityRef(_).ask[Option[ExaminationRegulation]](replyTo => GetExamreg(replyTo))))
        .flatMap { seq =>
          Future.sequence(seq)
            .map { seq =>
              seq
                .filter(opt => opt.isDefined) //Filter every not existing examination regulation
                .map(opt => opt.get)
                .filter(examReg => regulations.isEmpty || regulations.get.toLowerCase.split(",").contains(examReg.name.toLowerCase))
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
          .filter(module => moduleIds.isEmpty || moduleIds.get.toLowerCase.split(",").contains(module.id.toLowerCase))
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
  override def addExaminationRegulation(): ServiceCall[ExaminationRegulation, ExaminationRegulation] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall {
      (_, rawExaminationRegulation) =>
        val examinationRegulationProposal = rawExaminationRegulation.clean

        var validationErrors = try {
          Await.result(examinationRegulationProposal.validate, validationTimeout)
        }
        catch {
          case _: TimeoutException => throw UC4Exception.ValidationTimeout
          case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
        }

        val ref = entityRef(examinationRegulationProposal.name)
        ref.ask[Option[ExaminationRegulation]](replyTo => GetExamreg(replyTo)).flatMap { optExamreg =>
          if (optExamreg.isDefined) {
            validationErrors :+= SimpleError("name", "An examination regulation with this name does already exist.")
          }
          if (validationErrors.nonEmpty) {
            throw UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
          }

          val hlRef = entityRefHyperledger
          hlRef.askWithStatus[Confirmation](replyTo => CreateExamregHyperledger(examinationRegulationProposal, replyTo)).flatMap {
            case Accepted(_) =>
              val ref = entityRef(examinationRegulationProposal.name)

              ref.ask[Confirmation](replyTo => CreateExamregDatabase(examinationRegulationProposal, replyTo)).map {
                case Accepted(_) =>
                  (ResponseHeader(
                    201,
                    MessageProtocol.empty,
                    List(("Location", s"$pathPrefix/examination-regulations?regulations=${examinationRegulationProposal.name}"))
                  ), examinationRegulationProposal)

                case Rejected(statusCode, reason) => throw UC4Exception(statusCode, reason)
              }

            case Rejected(statusCode, reason) => throw UC4Exception(statusCode, reason)
          }.recover(handleException("Error while trying to add an examination regulation."))
        }
    }

  }

  /** @inheritdoc */
  override def closeExaminationRegulation(examregName: String): ServiceCall[NotUsed, Done] = authenticated(AuthenticationRole.Admin) { _ =>
    val ref = entityRef(examregName)
    ref.ask[Option[ExaminationRegulation]](replyTo => GetExamreg(replyTo)).flatMap { optExamReg =>
      if (optExamReg.isEmpty) {
        throw UC4Exception.NotFound
      }
      if (!optExamReg.get.active) {
        throw UC4Exception.AlreadyDeleted
      }
      val hlRef = entityRefHyperledger
      hlRef.askWithStatus[Confirmation](replyTo => CloseExamregHyperledger(examregName, replyTo)).flatMap {
        case Accepted(_) =>
          val ref = entityRef(examregName)
          ref.ask[Confirmation](replyTo => CloseExamregDatabase(replyTo)).map {
            case Accepted(_)                  => Done
            case Rejected(statusCode, reason) => throw UC4Exception(statusCode, reason)
          }
        case Rejected(statusCode, reason) => throw UC4Exception(statusCode, reason)
      }.recover(handleException("Error while trying to close an examination regulation."))
    }
  }

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** @inheritdoc */
  override def allowedMethodsGET: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** @inheritdoc */
  override def allowedMethodsGETPOST: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, POST")

  /** @inheritdoc */
  override def allowedMethodsDELETE: ServiceCall[NotUsed, Done] = allowedMethodsCustom("DELETE")
}
