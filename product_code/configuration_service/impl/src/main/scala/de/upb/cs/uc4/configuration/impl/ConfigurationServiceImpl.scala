package de.upb.cs.uc4.configuration.impl

import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.configuration.api.ConfigurationService
import de.upb.cs.uc4.configuration.model.{ Configuration, HyperledgerVersions, JsonSemester, ValidationConfiguration }
import de.upb.cs.uc4.shared.client.Utils
import de.upb.cs.uc4.shared.client.configuration.{ ConfigurationCollection, CourseLanguage, CourseType, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.{ SimpleError, UC4Exception }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import play.api.Environment

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

/** Implementation of the ConfigurationService */
class ConfigurationServiceImpl(override val environment: Environment)(implicit ec: ExecutionContext, config: Config)
  extends ConfigurationService {

  implicit val timeout: Timeout = Timeout(15.seconds)

  /** Get hyperledger versions */
  override def getHyperledgerVersions: ServiceCall[NotUsed, HyperledgerVersions] = ServiceCall { _ =>
    throw UC4Exception.NotImplemented
  }

  /** Get configuration */
  override def getConfiguration: ServiceCall[NotUsed, Configuration] = ServerServiceCall { (header, _) =>
    Future.successful(
      createETagHeader(
        header,
        Configuration(
          ConfigurationCollection.fieldOfStudies,
          ConfigurationCollection.countries,
          CourseLanguage.All.map(_.toString),
          CourseType.All.map(_.toString)
        )
      )
    )
  }

  /** Update the configuration */
  override def setConfiguration(): ServiceCall[Configuration, Done] = authenticated[Configuration, Done](AuthenticationRole.Admin) { _ =>
    throw UC4Exception.NotImplemented
  }

  /** Get validation specific configuration */
  override def getValidation: ServiceCall[NotUsed, ValidationConfiguration] = ServerServiceCall { (header, _) =>
    Future.successful(
      createETagHeader(header, ValidationConfiguration.build)
    )
  }

  /** Get semester based on data */
  override def getSemester(date: Option[String]): ServiceCall[NotUsed, JsonSemester] = ServiceCall[NotUsed, JsonSemester] { _ =>
    if (date.isEmpty) {
      throw UC4Exception.QueryParameterError(SimpleError("date", "Query parameter \"date\" must be set."))
    }
    if (!RegexCollection.Commons.dateRegex.matches(date.get)) {
      throw UC4Exception.QueryParameterError(SimpleError("date", "Date must be of the following format \"yyyy-mm-dd\"."))
    }

    Future.successful(JsonSemester(Utils.dateToSemester(date.get)))
  }

  override def allowedMethodsGET: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  override def allowedMethodsGETPUT: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, PUT")

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

}
