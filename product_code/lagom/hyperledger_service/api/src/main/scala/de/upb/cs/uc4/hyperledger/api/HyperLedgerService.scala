package de.upb.cs.uc4.hyperledger.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}

trait HyperLedgerService extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  val pathPrefix = "/hyperledger-management"

  def write(): ServiceCall[String, Done]

  def read(key: String): ServiceCall[NotUsed, String]

  final override def descriptor: Descriptor = {
    import Service._
    named("hyperledger")
      .withCalls(
        restCall(Method.GET, pathPrefix + "/courses/:key", read _),
        restCall(Method.POST, pathPrefix + "/courses", write _),
      )
      .withAutoAcl(true)
  }
}
