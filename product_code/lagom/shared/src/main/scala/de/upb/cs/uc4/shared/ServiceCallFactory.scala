package de.upb.cs.uc4.shared

import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import org.slf4j.{Logger, LoggerFactory}

object ServiceCallFactory {

  private final val log: Logger = LoggerFactory.getLogger("Shared")

  /** Wraps a [[com.lightbend.lagom.scaladsl.api.ServiceCall]] into a Logger.
    * Logs the header method and header uri of any incoming call.
    *
    * @param serviceCall which should get wrapped
    * @return finished [[com.lightbend.lagom.scaladsl.server.ServerServiceCall]]
    */
  def logged[Request, Response](serviceCall: ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] =
    ServerServiceCall.compose { requestHeader =>
      log.info("Received {} {}", requestHeader.method, requestHeader.uri)
      serviceCall
    }
}
