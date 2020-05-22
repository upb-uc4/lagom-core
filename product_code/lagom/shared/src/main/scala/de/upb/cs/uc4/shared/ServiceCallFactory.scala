package de.upb.cs.uc4.shared

import com.lightbend.lagom.scaladsl.server.ServerServiceCall

object ServiceCallFactory {

  /** Wraps a [[com.lightbend.lagom.scaladsl.api.ServiceCall]] into a Logger.
    * Logs the header method and header uri of any incoming call.
    *
    * @param serviceCall which should get wrapped
    * @return finished [[com.lightbend.lagom.scaladsl.server.ServerServiceCall]]
    */
  def logged[Request, Response](serviceCall: ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] =
    ServerServiceCall.compose { requestHeader =>
      println(s"Received ${requestHeader.method} ${requestHeader.uri}")
      serviceCall
    }
}
