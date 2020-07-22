package de.upb.cs.uc4.shared.server.hyperledger

import com.lightbend.lagom.scaladsl.client.ServiceClient
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import com.softwaremill.macwire.wire

import scala.concurrent.ExecutionContext

trait HyperledgerComponent {
  implicit val executionContext: ExecutionContext
  val serviceClient: ServiceClient
  lazy val hyperLedgerService: HyperLedgerService = serviceClient.implement[HyperLedgerService]
  lazy val hyperLedgerSession: HyperLedgerSession = wire[HyperLedgerSession]
}
