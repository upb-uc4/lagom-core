package de.upb.cs.uc4.shared.server

import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.typesafe.config.Config

/** Mixin to include into a Service, to enable authenticated ServiceCalls  */
trait AuthenticationComponent extends LagomConfigComponent {
  override implicit def config: Config = super.config
}
