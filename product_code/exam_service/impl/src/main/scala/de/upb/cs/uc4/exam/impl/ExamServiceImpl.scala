package de.upb.cs.uc4.exam.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.hyperledger.HyperledgerUtils
import de.upb.cs.uc4.hyperledger.commands.HyperledgerBaseCommand
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import org.slf4j.{Logger, LoggerFactory}
import play.api.Environment

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** Implementation of the ExamService */
class ExamServiceImpl(
    clusterSharding: ClusterSharding,
    courseService: CourseService,
    operationService: OperationService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends ExamService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(null, null) //TODO

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  private final val log: Logger = LoggerFactory.getLogger(classOf[ExamServiceImpl])

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = ServiceCall { _ =>
    HyperledgerUtils.VersionUtil.createHyperledgerVersionResponse(entityRef)
  }

  /** Returns Exams, optionally filtered */
  override def getExams(examIds: Option[String], courseIds: Option[String], lecturerIds: Option[String], moduleIds: Option[String], types: Option[String], admittableAt: Option[String], droppableAt: Option[String]): ServiceCall[NotUsed, Seq[Exam]] =  authenticated(AuthenticationRole.All: _*) {
    ServerServiceCall { (_, _) =>
      // TODO implement call
      // Just authenticated, no filtering by authUser or something, just good old fetch and filter
      throw UC4Exception.NotImplemented

    }
  }

  /** Get a proposal for adding an Exam */
  override def getProposalAddExam(): ServiceCall[Exam, UnsignedProposal] = identifiedAuthenticated(AuthenticationRole.Lecturer) {
    (authUser, role) =>
      ServerServiceCall { (header, _) =>
        // TODO implement call
        throw UC4Exception.NotImplemented
      }
  }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")
}
