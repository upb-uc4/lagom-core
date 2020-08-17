package de.upb.cs.uc4.hyperledger.api

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.shared.client.UC4Service

trait HyperLedgerService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix = "/hyperledger-management"
  /** The name of the service */
  override val name: String = "hyperledger"
  /** This services does not use auto acl */
  override val autoAcl: Boolean = false

  def write(transactionId: String): ServiceCall[Seq[String], Done]

  def read(transactionId: String): ServiceCall[Seq[String], String]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.POST, pathPrefix + "/read/:transactionId", read _),
        restCall(Method.POST, pathPrefix + "/write/:transactionId", write _)
      )
  }
}
