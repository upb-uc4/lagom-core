package de.upb.cs.uc4.shared.server

import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomApplicationContext }
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents
import play.filters.gzip.GzipFilterComponents

abstract class UC4Application(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CORSComponents
    with GzipFilterComponents
    with AhcWSComponents
    with AuthenticationComponent {

  // Set HttpFilter to the default CorsFilter and GzipFilter
  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter, gzipFilter)
}
