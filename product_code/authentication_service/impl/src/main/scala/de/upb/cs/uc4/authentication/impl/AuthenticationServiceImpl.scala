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
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.authentication.model._
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, SimpleError, UC4CriticalException, UC4Exception, UC4NonCriticalException }
import de.upb.cs.uc4.shared.server.Hashing
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, RejectedWithError }
import io.jsonwebtoken._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

class AuthenticationServiceImpl(readSide: ReadSide, processor: AuthenticationEventProcessor,
    clusterSharding: ClusterSharding, config: Config)(implicit ec: ExecutionContext) extends AuthenticationService {

  readSide.register(processor)

  private def entityRef(id: String): EntityRef[AuthenticationCommand] =
    clusterSharding.entityRefFor(AuthenticationState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.validation.timeout").milliseconds

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Sets the authentication data of a user */
  override def setAuthentication(): ServiceCall[AuthenticationUser, Done] = ServiceCall { user =>
    entityRef(Hashing.sha256(user.username)).ask[Confirmation](replyTo => SetAuthentication(user, replyTo)).map {
      case Accepted => Done
      case RejectedWithError(code, errorResponse) =>
        throw UC4Exception(code, errorResponse)
    }
  }

  /** Changes the password of the given user */
  override def changePassword(username: String): ServiceCall[AuthenticationUser, Done] =
    identifiedAuthenticated[AuthenticationUser, Done](AuthenticationRole.All: _*) {
      (authUsername, role) =>
        ServerServiceCall { (_: RequestHeader, authUserRaw: AuthenticationUser) =>
          val authUser = authUserRaw.clean

          if (username != authUser.username) {
            throw UC4Exception.PathParameterMismatch
          }
          if (authUsername != authUser.username) {
            throw UC4Exception.OwnerMismatch
          }

          val validationErrors = try {
            Await.result(authUser.validate, validationTimeout)
          }
          catch {
            case _: TimeoutException => throw UC4Exception.ValidationTimeout
            case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
          }

          if (validationErrors.nonEmpty) {
            throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
          }

          if (role != authUser.role) {
            throw new UC4NonCriticalException(422, DetailedError(ErrorType.UneditableFields, List(SimpleError("role", "Role may not be manually changed."))))
          }

          val ref = entityRef(Hashing.sha256(authUser.username))

          ref.ask[Confirmation](replyTo => SetAuthentication(authUser, replyTo))
            .map {
              case Accepted => // Update Successful
                (ResponseHeader(200, MessageProtocol.empty, List()), Done)
              case RejectedWithError(code, errorResponse) =>
                throw UC4Exception(code, errorResponse)
            }
        }
    }(config, ec)

  /** Logs a user in and returns a refresh and a login token in the header */
  override def login: ServiceCall[NotUsed, Done] = ServerServiceCall { (header, _) =>
    val (username, password) = getUserAndPassword(header)

    entityRef(Hashing.sha256(username)).ask[Option[AuthenticationEntry]](replyTo => GetAuthentication(replyTo)).map {
      case Some(entry) =>
        if (entry.password != Hashing.sha256(entry.salt + password)) {
          throw UC4Exception.BasicAuthorizationError
        }
        else {
          val (refreshToken, date) = createRefreshToken(username, entry.role)
          val (loginToken, logoutTimer) = createLoginToken(username, entry.role)

          (ResponseHeader(200, MessageProtocol.empty, List(
            ("Set-Cookie", s"refresh=$refreshToken; SameSite=Strict; Secure; HttpOnly; Expires=$date ;;" +
              s"login=$loginToken; SameSite=Strict; Secure; HttpOnly; Max-Age=$logoutTimer")
          )), Done)
        }
      case None =>
        throw UC4Exception.BasicAuthorizationError
    }
  }

  /** Generates a new login token from a refresh token */
  override def refresh: ServiceCall[NotUsed, JsonUsername] = ServerServiceCall { (header, _) =>
    header.getHeader("Cookie") match {
      case Some(cookies) => cookies.split(";").map(_.trim).find(_.startsWith("refresh=")) match {
        case Some(s"refresh=$token") =>
          val (loginToken, logoutTimer, username) = createLoginTokenFromRefreshToken(token)

          Future.successful(
            (ResponseHeader(200, MessageProtocol.empty, List(
              ("Set-Cookie", s"login=$loginToken; SameSite=Strict; Secure; HttpOnly; Max-Age=$logoutTimer")
            )), JsonUsername(username))
          )
        case _ => throw UC4Exception.RefreshTokenMissing
      }
      case _ => throw UC4Exception.RefreshTokenMissing
    }
  }

  /** Logs a user in and returns a refresh and a login token in the body */
  override def loginMachineUser: ServiceCall[NotUsed, Tokens] = ServerServiceCall { (header, _) =>
    val (username, password) = getUserAndPassword(header)

    entityRef(Hashing.sha256(username)).ask[Option[AuthenticationEntry]](replyTo => GetAuthentication(replyTo)).map {
      case Some(entry) =>
        if (entry.password != Hashing.sha256(entry.salt + password)) {
          throw UC4Exception.BasicAuthorizationError
        }
        else {
          val (refreshToken, _) = createRefreshToken(username, entry.role)
          val (loginToken, _) = createLoginToken(username, entry.role)

          (ResponseHeader(200, MessageProtocol.empty, List()), Tokens(loginToken, refreshToken))
        }
      case None =>
        throw UC4Exception.BasicAuthorizationError
    }
  }

  /** Generates a new login token from a refresh token in the bearer header */
  override def refreshMachineUser: ServiceCall[NotUsed, RefreshToken] = ServerServiceCall { (header, _) =>
    val refreshToken = try {
      getBearerToken(header)
    }
    catch {
      case ce: UC4Exception if ce.possibleErrorResponse.`type` == ErrorType.JwtAuthorization =>
        throw UC4Exception.RefreshTokenMissing
      case e: Throwable => throw e
    }
    val (loginToken, _, username) = createLoginTokenFromRefreshToken(refreshToken)

    Future.successful(
      (ResponseHeader(200, MessageProtocol.empty, List()), RefreshToken(loginToken, username))
    )
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

  /** Reads username and password from the header
    *
    * @param requestHeader with the an authentication header
    * @return a String tuple with username and password
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
      throw UC4Exception.BasicAuthorizationError
    }
    else {
      userPw.get
    }
  }

  /** Reads bearer token from the header
    *
    * @param requestHeader with the an authentication header
    * @return the token as a string
    */
  private def getBearerToken(requestHeader: RequestHeader): String = {
    val token = requestHeader.getHeader("Authorization").getOrElse("").split("\\s+") match {
      case Array("Bearer", token) => Some(token)
      case _ => None
    }

    if (token.isEmpty) {
      throw UC4Exception.JwtAuthorizationError
    }
    else {
      token.get
    }
  }

  /** Create a refresh token out of the given parameters
    *
    * @param username of the token owner
    * @param role     of the token owner
    * @return a String tuple with the token as a string and the formatted expiration date
    */
  private def createRefreshToken(username: String, role: AuthenticationRole): (String, String) = {
    val key = config.getString("play.http.secret.key")

    val nowRefresh = Calendar.getInstance()
    nowRefresh.add(Calendar.DATE, config.getInt("uc4.authentication.refresh"))

    val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))

    val refreshToken = Jwts.builder()
      .setSubject("refresh")
      .setExpiration(nowRefresh.getTime)
      .claim("username", username)
      .claim("authenticationRole", role.toString)
      .signWith(SignatureAlgorithm.HS256, key)
      .compact()

    (refreshToken, dateFormat.format(nowRefresh.getTime))
  }

  /** Create a login token out of the given parameters
    *
    * @param username of the token owner
    * @param role     of the token owner
    * @return a tuple with the token as a string and the expiration time in minutes
    */
  private def createLoginToken(username: String, role: AuthenticationRole): (String, Int) = {
    val key = config.getString("play.http.secret.key")

    val now = Calendar.getInstance()
    val logoutTimer = config.getInt("uc4.authentication.login")
    now.add(Calendar.MINUTE, logoutTimer)

    val loginToken = Jwts.builder()
      .setSubject("login")
      .setExpiration(now.getTime)
      .claim("username", username)
      .claim("authenticationRole", role.toString)
      .signWith(SignatureAlgorithm.HS256, key)
      .compact()

    (loginToken, logoutTimer * 60)
  }

  /** Create a login token out of the given refresh token
    *
    * @param refreshToken of the token owner
    * @return a tuple with the token as a string, the expiration time in minutes and the username of the token owner
    */
  private def createLoginTokenFromRefreshToken(refreshToken: String): (String, Int, String) = {
    try {
      val key = config.getString("play.http.secret.key")
      val claims = Jwts.parser().setSigningKey(key).parseClaimsJws(refreshToken).getBody
      val username = claims.get("username", classOf[String])
      val authenticationRole = claims.get("authenticationRole", classOf[String])
      val subject = claims.getSubject

      if (subject != "refresh") {
        throw UC4Exception.RefreshTokenMissing
      }

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

      (loginToken, logoutTimer * 60, username)
    }
    catch {
      case _: ExpiredJwtException      => throw UC4Exception.RefreshTokenExpired
      case _: UnsupportedJwtException  => throw UC4Exception.MalformedRefreshToken
      case _: MalformedJwtException    => throw UC4Exception.MalformedRefreshToken
      case _: SignatureException       => throw UC4Exception.RefreshTokenSignatureError
      case _: IllegalArgumentException => throw UC4Exception.RefreshTokenMissing
      case ue: UC4Exception            => throw ue
      case ex: Throwable               => throw UC4Exception.InternalServerError("Failed to create LoginToken", ex.getMessage, ex)
    }
  }
}
