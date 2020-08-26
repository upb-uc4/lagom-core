package de.upb.cs.uc4.authentication.impl

import java.text.SimpleDateFormat
import java.util.{ Base64, Calendar, Locale, TimeZone }

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.impl.actor.{ AuthenticationEntry, AuthenticationState }
import de.upb.cs.uc4.authentication.impl.commands.{ AuthenticationCommand, GetAuthentication, SetAuthentication }
import de.upb.cs.uc4.authentication.impl.readside.AuthenticationEventProcessor
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser, JsonUsername }
import de.upb.cs.uc4.shared.client.exceptions.{ CustomException, DetailedError, SimpleError }
import de.upb.cs.uc4.shared.server.Hashing
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, RejectedWithError }
import io.jsonwebtoken._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class AuthenticationServiceImpl(readSide: ReadSide, processor: AuthenticationEventProcessor,
    clusterSharding: ClusterSharding, config: Config)(implicit ec: ExecutionContext) extends AuthenticationService {

  readSide.register(processor)

  private def entityRef(id: String): EntityRef[AuthenticationCommand] =
    clusterSharding.entityRefFor(AuthenticationState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Sets the authentication data of a user */
  override def setAuthentication(): ServiceCall[AuthenticationUser, Done] = ServiceCall { user =>
    entityRef(Hashing.sha256(user.username)).ask[Confirmation](replyTo => SetAuthentication(user, replyTo)).map {
      case Accepted => Done
      case RejectedWithError(code, errorResponse) =>
        throw new CustomException(code, errorResponse)
    }
  }

  /** Changes the password of the given user */
  override def changePassword(username: String): ServiceCall[AuthenticationUser, Done] =
    identifiedAuthenticated[AuthenticationUser, Done](AuthenticationRole.All: _*) {
      (authUsername, role) =>
        ServerServiceCall { (_: RequestHeader, user: AuthenticationUser) =>
          if (username != user.username.trim) {
            throw CustomException.PathParameterMismatch
          }
          if (authUsername != user.username.trim) {
            throw CustomException.OwnerMismatch
          }
          if (role != user.role) {
            throw new CustomException(422, DetailedError("uneditable fields", List(SimpleError("role", "Role may not be manually changed."))))
          }
          val ref = entityRef(Hashing.sha256(user.username))

          ref.ask[Confirmation](replyTo => SetAuthentication(user, replyTo))
            .map {
              case Accepted => // Update Successful
                (ResponseHeader(200, MessageProtocol.empty, List()), Done)
              case RejectedWithError(code, errorResponse) =>
                throw new CustomException(code, errorResponse)
            }
        }
    }(config, ec)

  /** Logs a user in and returns a refresh and a login token in the header */
  override def login: ServiceCall[NotUsed, Done] = ServerServiceCall { (header, _) =>
    val (username, password) = getUserAndPassword(header)

    entityRef(Hashing.sha256(username)).ask[Option[AuthenticationEntry]](replyTo => GetAuthentication(replyTo)).map {
      case Some(entry) =>
        if (entry.password != Hashing.sha256(entry.salt + password)) {
          throw CustomException.BasicAuthorizationError
        }
        else {
          val key = config.getString("play.http.secret.key")

          val nowRefresh = Calendar.getInstance()
          nowRefresh.add(Calendar.DATE, config.getInt("uc4.authentication.refresh"))
          val refreshToken =
            Jwts.builder()
              .setSubject("refresh")
              .setExpiration(nowRefresh.getTime)
              .claim("username", username)
              .claim("authenticationRole", entry.role.toString)
              .signWith(SignatureAlgorithm.HS256, key)
              .compact()

          val nowLogin = Calendar.getInstance()
          val logoutTimer = config.getInt("uc4.authentication.login")
          nowLogin.add(Calendar.MINUTE, logoutTimer)
          val loginToken =
            Jwts.builder()
              .setSubject("login")
              .setExpiration(nowLogin.getTime)
              .claim("username", username)
              .claim("authenticationRole", entry.role.toString)
              .signWith(SignatureAlgorithm.HS256, key)
              .compact()

          val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
          dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))

          (ResponseHeader(200, MessageProtocol.empty, List(
            ("Set-Cookie", s"refresh=$refreshToken; SameSite=Strict; Secure; HttpOnly; Expires=${dateFormat.format(nowRefresh.getTime)} ;;" +
              s"login=$loginToken; SameSite=Strict; Secure; HttpOnly; Max-Age=${logoutTimer * 60}")
          )), Done)
        }
      case None =>
        throw CustomException.BasicAuthorizationError
    }
  }

  /** Generates a new login token out of a refresh token */
  override def refresh: ServiceCall[NotUsed, JsonUsername] = ServerServiceCall { (header, _) =>
    header.getHeader("Cookie") match {
      case Some(cookies) => cookies.split(";").map(_.trim).find(_.startsWith("refresh=")) match {
        case Some(s"refresh=$token") =>
          try {
            val key = config.getString("play.http.secret.key")
            val claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody
            val username = claims.get("username", classOf[String])
            val authenticationRole = claims.get("authenticationRole", classOf[String])

            val now = Calendar.getInstance()
            val logoutTimer = config.getInt("uc4.authentication.login")
            now.add(Calendar.MINUTE, logoutTimer)
            val loginToken =
              Jwts.builder()
                .setSubject("login")
                .setExpiration(now.getTime)
                .claim("username", username)
                .claim("authenticationRole", authenticationRole)
                .signWith(SignatureAlgorithm.HS256, key)
                .compact()

            Future.successful(
              (ResponseHeader(200, MessageProtocol.empty, List(
                ("Set-Cookie", s"login=$loginToken; SameSite=Strict; Secure; HttpOnly; Max-Age=${logoutTimer * 60}")
              )), JsonUsername(username))
            )
          }
          catch {
            case _: ExpiredJwtException   => throw CustomException.RefreshTokenExpired
            case _: MalformedJwtException => throw CustomException.MalformedRefreshToken
            case _: SignatureException    => throw CustomException.RefreshTokenSignatureError
            case _: Exception             => throw CustomException.InternalServerError
          }
        case _ => throw CustomException.AuthorizationError
      }
      case _ => throw CustomException.AuthorizationError
    }
  }

  /** Logs the user out */
  override def logout: ServiceCall[NotUsed, Done] = ServerServiceCall { (_, _) =>
    val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))

    Future.successful(
      (ResponseHeader(200, MessageProtocol.empty, List(
        ("Set-Cookie", s"refresh=; SameSite=Strict; Secure; HttpOnly; Expires=${dateFormat.format(Calendar.getInstance().getTime)} ;;" +
          s"login=; SameSite=Strict; Secure; HttpOnly; Max-Age=0")
      )), Done)
    )
  }

  /** Allows PUT */
  override def allowedPut: ServiceCall[NotUsed, Done] = allowedMethodsCustom("PUT")

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Reads username and password out of the header
    *
    * @param requestHeader with the an authentication header
    * @return an Option with a String tuple
    */
  private def getUserAndPassword(requestHeader: RequestHeader): (String, String) = {
    val userPw = requestHeader.getHeader("Authorization").getOrElse("").split("\\s+") match {
      case Array("Basic", userAndPass) =>
        new String(Base64.getDecoder.decode(userAndPass), "UTF-8").split(":") match {
          case Array(user, password) => Option(user, password)
          case _                     => None
        }
      case _ => None
    }

    if (userPw.isEmpty) {
      throw CustomException.BasicAuthorizationError
    }
    else {
      userPw.get
    }
  }
}
