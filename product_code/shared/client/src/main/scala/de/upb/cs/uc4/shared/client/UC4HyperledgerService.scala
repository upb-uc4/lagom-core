package de.upb.cs.uc4.shared.client

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }

trait UC4HyperledgerService extends UC4Service {

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
