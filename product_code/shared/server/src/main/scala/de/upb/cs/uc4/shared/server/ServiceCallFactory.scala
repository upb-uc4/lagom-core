package de.upb.cs.uc4.shared.server

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import io.jsonwebtoken._
import org.slf4j.{ Logger, LoggerFactory }

import scala.annotation.varargs
import scala.concurrent.{ ExecutionContext, Future }

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
      log.info("Received {} {} with headers {}", requestHeader.method, requestHeader.uri, requestHeader.headers)
      serviceCall
    }

  /** Wraps a [[com.lightbend.lagom.scaladsl.api.ServiceCall]] to make it authenticated.
    * Authentication checks username, password and role (to check privileges).
    *
    * @param serviceCall which should get wrapped
    * @return finished [[com.lightbend.lagom.scaladsl.server.ServerServiceCall]]
    */
  @varargs
  def authenticated[Request, Response](roles: AuthenticationRole*)(serviceCall: ServerServiceCall[Request, Response])(implicit config: Config, ec: ExecutionContext): ServerServiceCall[Request, Response] = {
    ServerServiceCall.compose[Request, Response] { requestHeader =>
      val (_, role) = checkLoginToken(getLoginToken(requestHeader))
      if (!roles.contains(role)) {
        throw UC4Exception.NotEnoughPrivileges
      }
      serviceCall
    }
  }

  /** Wraps a [[com.lightbend.lagom.scaladsl.api.ServiceCall]] to make it authenticated
    * and forwards the username and the role to the serviceCall.
    * Authentication checks username, password and role (to check privileges).
    *
    * @param serviceCall which should get wrapped
    * @return finished [[com.lightbend.lagom.scaladsl.server.ServerServiceCall]]
    */
  @varargs
  def identifiedAuthenticated[Request, Response](roles: AuthenticationRole*)(serviceCall: (String, AuthenticationRole) => ServerServiceCall[Request, Response])(implicit config: Config, ec: ExecutionContext): ServerServiceCall[Request, Response] = {
    ServerServiceCall.compose[Request, Response] { requestHeader =>
      val (username, role) = checkLoginToken(getLoginToken(requestHeader))
      if (!roles.contains(role)) {
        throw UC4Exception.NotEnoughPrivileges
      }
      serviceCall(username, role)
    }
  }

  /** Checks if a token is a valid token
    *
    * @param token to check
    * @param config configuration of the application
    * @return the username and the authentication role of the owner of the token
    */
  private def checkLoginToken(token: String)(implicit config: Config): (String, AuthenticationRole) = {
    try {
      val claims = Jwts.parser().setSigningKey(config.getString("play.http.secret.key")).parseClaimsJws(token).getBody
      val username = claims.get("username", classOf[String])
      val authenticationRole = claims.get("authenticationRole", classOf[String])
      val subject = claims.getSubject

      if (subject != "login") {
        throw UC4Exception.JwtAuthorizationError
      }

      (username, AuthenticationRole.withName(authenticationRole))
    }
    catch {
      case _: ExpiredJwtException      => throw UC4Exception.RefreshTokenExpired
      case _: UnsupportedJwtException  => throw UC4Exception.MalformedRefreshToken
      case _: MalformedJwtException    => throw UC4Exception.MalformedRefreshToken
      case _: SignatureException       => throw UC4Exception.RefreshTokenSignatureError
      case _: IllegalArgumentException => throw UC4Exception.JwtAuthorizationError
      case ue: UC4Exception            => throw ue
      case ex: Exception               => throw UC4Exception.InternalServerError("LoginToken Check Error", ex.getMessage, ex)
    }
  }

  /** Reads the login token out of the header
    *
    * @param requestHeader with the a cookie header or an authorization header
    * @return the token as string
    */
  private def getLoginToken(requestHeader: RequestHeader): String = {
    val cookieToken = requestHeader.getHeader("Cookie") match {
      case Some(cookies) => cookies.split(";").map(_.trim).find(_.startsWith("login=")) match {
        case Some(s"login=$token") => Some(token)
        case _                     => None
      }
      case _ => None
    }

    val authorizationToken = requestHeader.getHeader("Authorization") match {
      case Some(header) =>
        header.split("\\s+") match {
          case Array("Bearer", token) => Some(token)
          case _ => None
        }
      case _ => None
    }

    if (cookieToken.isDefined && authorizationToken.isDefined) {
      throw UC4Exception.MultipleAuthorizationError
    }
    else if (cookieToken.isDefined) {
      cookieToken.get
    }
    else if (authorizationToken.isDefined) {
      authorizationToken.get
    }
    else {
      throw UC4Exception.JwtAuthorizationError
    }
  }

  /** ServiceCall that returns a list of allowed methods, all of which will also be listed as access controlled
    *
    * @param listOfOptions with the allowed options. Schema: "GET, POST, DELETE"
    *                      OPTIONS is added automatically
    */
  def allowedMethodsCustom(listOfOptions: String): ServiceCall[NotUsed, Done] = ServerServiceCall {
    (_, _) =>
      Future.successful {
        (ResponseHeader(200, MessageProtocol.empty, List(
          ("Allow", listOfOptions + ", OPTIONS"),
          ("Access-Control-Allow-Methods", listOfOptions + ", OPTIONS"),
          ("Accept-Encoding", "gzip")
        )), Done)
      }
  }
}
