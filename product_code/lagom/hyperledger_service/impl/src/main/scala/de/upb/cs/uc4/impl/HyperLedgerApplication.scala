package de.upb.cs.uc4.impl

import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.api.HyperLedgerService
import play.api.libs.ws.ahc.AhcWSComponents

abstract class HyperLedgerApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HyperLedgerService](wire[HyperLedgerServiceImpl])
}
