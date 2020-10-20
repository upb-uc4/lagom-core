package de.upb.cs.uc4.configuration.impl

import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.configuration.api.ConfigurationService
import de.upb.cs.uc4.shared.server.UC4Application

abstract class ConfigurationApplication(context: LagomApplicationContext)
  extends UC4Application(context) {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[ConfigurationService](wire[ConfigurationServiceImpl])

}

