package de.upb.cs.uc4.matriculation.impl

import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.shared.server.AuthenticationComponent
import de.upb.cs.uc4.shared.server.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.user.api.UserService
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

abstract class MatriculationApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CORSComponents
    with AhcWSComponents
    with AuthenticationComponent
    with HyperledgerComponent {

  // Set HttpFilter to the default CorsFilter
  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Bind UserService
  lazy val userService: UserService = serviceClient.implement[UserService]

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[MatriculationService](wire[MatriculationServiceImpl])
}
