package de.upb.cs.uc4.examresult.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.stream.Materializer
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.examresult.api.ExamResultService
import de.upb.cs.uc4.examresult.model.{ ExamResult, ExamResultEntry }
import de.upb.cs.uc4.hyperledger.HyperledgerUtils
import de.upb.cs.uc4.hyperledger.commands.HyperledgerBaseCommand
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** Implementation of the ExamService */
class ExamResultServiceImpl(
    clusterSharding: ClusterSharding,
    operationService: OperationService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends ExamResultService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(null, null) //TODO

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  private final val log: Logger = LoggerFactory.getLogger(classOf[ExamResultServiceImpl])

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = ServiceCall { _ =>
    HyperledgerUtils.VersionUtil.createHyperledgerVersionResponse(entityRef)
  }

  /** Returns ExamResultEntries, optionally filtered */
  override def getExamResults(username: Option[String], examIds: Option[String]): ServiceCall[NotUsed, Seq[ExamResultEntry]] = ???

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")

  /** Get a proposals for adding the result of an exam */
  override def getProposalAddExamResult(): ServiceCall[ExamResult, UnsignedProposal] = ???
}
