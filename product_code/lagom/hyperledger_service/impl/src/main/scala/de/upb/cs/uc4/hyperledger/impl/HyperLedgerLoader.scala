package de.upb.cs.uc4.hyperledger.impl

import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService

class HyperLedgerLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HyperLedgerApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HyperLedgerApplication(context) with LagomDevModeComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[HyperLedgerService])
}
