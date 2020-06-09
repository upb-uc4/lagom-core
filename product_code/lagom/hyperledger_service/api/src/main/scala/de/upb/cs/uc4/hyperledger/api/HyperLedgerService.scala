package de.upb.cs.uc4.hyperledger.api

import com.lightbend.lagom.scaladsl.api.{Descriptor, Service}

trait HyperLedgerService extends Service {

  final override def descriptor: Descriptor = {
    import Service._
    named("HyperLedgerApi")
  }
}
