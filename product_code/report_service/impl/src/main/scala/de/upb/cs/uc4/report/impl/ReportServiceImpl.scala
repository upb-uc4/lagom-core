package de.upb.cs.uc4.report.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.ReportState
import de.upb.cs.uc4.report.impl.commands.ReportCommand
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import de.upb.cs.uc4.user.api.UserService
import play.api.Environment
import slick.lifted.Functions.database

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/** Implementation of the ReportService */
class ReportServiceImpl(
    clusterSharding: ClusterSharding,
    userService: UserService,
    courseService: CourseService,
    matriculationService: MatriculationService,
    certificateService: CertificateService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config) extends ReportService {

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[ReportCommand] =
    clusterSharding.entityRefFor(ReportState.typeKey, id)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.database").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  /** Request collection of all data for the given user */
  override def prepareUserData(username: String): ServiceCall[NotUsed, Done] = ???

  /** Get all data for the specified user */
  override def getUserData(username: String): ServiceCall[NotUsed, Array[Byte]] = ???

  /** Allows GET */
  override def allowedMethodsGET: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")


}
