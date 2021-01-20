package de.upb.cs.uc4.hyperledger.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.hyperledger.api.model.JsonHyperledgerVersion
import de.upb.cs.uc4.shared.client.UC4Service

trait UC4HyperledgerService extends UC4Service {

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion]

  override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/version/hyperledger", getHlfVersions _),
        restCall(Method.OPTIONS, pathPrefix + "/version/hyperledger", allowVersionNumber _)
      )
  }
}
