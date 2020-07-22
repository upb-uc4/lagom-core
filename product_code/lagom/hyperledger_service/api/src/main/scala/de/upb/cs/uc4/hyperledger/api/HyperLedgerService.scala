package de.upb.cs.uc4.hyperledger.api

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.upb.cs.uc4.shared.client.CustomExceptionSerializer
import play.api.Environment

trait HyperLedgerService extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  val pathPrefix = "/hyperledger-management"

  def write(transactionId: String): ServiceCall[Seq[String], Done]

  def read(transactionId: String): ServiceCall[Seq[String], String]

  final override def descriptor: Descriptor = {
    import Service._
    named("hyperledger")
      .withCalls(
        restCall(Method.POST, pathPrefix + "/read/:transactionId", read _),
        restCall(Method.POST, pathPrefix + "/write/:transactionId", write _),
      ).withExceptionSerializer(new CustomExceptionSerializer(Environment.simple()))
  }
}
