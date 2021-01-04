package de.upb.cs.uc4.configuration.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.configuration.model.{ Configuration, JsonSemester, ValidationConfiguration }
import de.upb.cs.uc4.shared.client.{ JsonHyperledgerNetworkVersion, UC4Service }

object ConfigurationService {
  val TOPIC_NAME = "Configuration"
}

/** The ConfigurationService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the ConfigurationService.
  */
trait ConfigurationService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix = "/configuration-management"
  override val name = "configuration"

  /** Get hyperledger network version */
  def getHyperledgerNetworkVersion: ServiceCall[NotUsed, JsonHyperledgerNetworkVersion]

  /** Get configuration */
  def getConfiguration: ServiceCall[NotUsed, Configuration]

  /** Update the configuration */
  def setConfiguration(): ServiceCall[Configuration, Done]

  /** Get validation specific configuration */
  def getValidation: ServiceCall[NotUsed, ValidationConfiguration]

  /** Get semester based on data */
  def getSemester(date: Option[String]): ServiceCall[NotUsed, JsonSemester]

  /** Allows GET */
  def allowedMethodsGET: ServiceCall[NotUsed, Done]

  /** Allows GET PUT */
  def allowedMethodsGETPUT: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/version/hyperledger-network", getHyperledgerNetworkVersion _),
        restCall(Method.GET, pathPrefix + "/configuration", getConfiguration _),
        restCall(Method.PUT, pathPrefix + "/configuration", setConfiguration _),
        restCall(Method.GET, pathPrefix + "/validation", getValidation _),
        restCall(Method.GET, pathPrefix + "/semester?date", getSemester _),
        restCall(Method.OPTIONS, pathPrefix + "/version/hyperledger-network", allowedMethodsGET _),
        restCall(Method.OPTIONS, pathPrefix + "/configuration", allowedMethodsGETPUT _),
        restCall(Method.OPTIONS, pathPrefix + "/validation", allowedMethodsGET _),
        restCall(Method.OPTIONS, pathPrefix + "/semester", allowedMethodsGET _)
      )
  }
}
