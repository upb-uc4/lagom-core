package de.upb.cs.uc4.hl.course.impl

import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.hl.course.api.HlCourseService
import de.upb.cs.uc4.shared.server.AuthenticationComponent
import de.upb.cs.uc4.shared.server.hyperledger.HyperledgerComponent
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

abstract class HlCourseApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
  with CORSComponents
  with AhcWSComponents
  with AuthenticationComponent
  with HyperledgerComponent {

  // Set HttpFilter to the default CorsFilter
  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HlCourseService](wire[HlCourseServiceImpl])
}
