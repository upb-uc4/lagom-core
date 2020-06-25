package de.upb.cs.uc4.shared

import java.util.Base64

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.varargs
import scala.concurrent.{ExecutionContext, Future}

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

  /** Wraps a [[com.lightbend.lagom.scaladsl.api.ServiceCall]] to make it authenticated.
    * Authentication checks username, password and role (to check privileges).
    *
    * @param serviceCall which should get wrapped
    * @return finished [[com.lightbend.lagom.scaladsl.server.ServerServiceCall]]
    */
  @varargs
  def authenticated[Request, Response](role: AuthenticationRole*)(serviceCall: ServerServiceCall[Request, Response])
                                      (implicit auth: AuthenticationService, ec: ExecutionContext)
  : ServerServiceCall[Request, Response] = {
    ServerServiceCall.composeAsync[Request, Response] { requestHeader =>
      val (user, pw) = extractUserAndPassword(requestHeader)

      auth.check(user, pw).invoke(role).map {
        case AuthenticationResponse.Correct => serviceCall
        case AuthenticationResponse.WrongUsername => throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"), new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
        case AuthenticationResponse.WrongPassword => throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"), new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
        case AuthenticationResponse.NotAuthorized => throw Forbidden("Not enough privileges for this call.")
      }
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
  def identifiedAuthenticated[Request, Response](role: AuthenticationRole*)
                                                (serviceCall: (String, AuthenticationRole) => ServerServiceCall[Request, Response])
                                                (implicit auth: AuthenticationService, ec: ExecutionContext)
  : ServerServiceCall[Request, Response] = {
    ServerServiceCall.composeAsync[Request, Response] { requestHeader =>
      val (user, pw) = extractUserAndPassword(requestHeader)

      auth.check(user, pw).invoke(role).flatMap {
        case AuthenticationResponse.Correct => auth.getRole(user).invoke().map(role => serviceCall(user, role))
        case AuthenticationResponse.WrongUsername => throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"), new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
        case AuthenticationResponse.WrongPassword => throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"), new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
        case AuthenticationResponse.NotAuthorized => throw Forbidden("Not enough privileges for this call.")
      }
    }
  }

  def extractUserAndPassword(requestHeader: RequestHeader): (String, String) = {
    val userPw = getUserAndPassword(requestHeader)

    if (userPw.isEmpty) {
      throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"), new ExceptionMessage("Unauthorized", "No Authorization given"))
    } else {
      userPw.get
    }
  }

  /**
    * Reads username and password out of the header
    *
    * @param requestHeader with the an authentication header
    * @return an Option with a String tuple
    */
  def getUserAndPassword(requestHeader: RequestHeader): Option[(String, String)] = {
    requestHeader.getHeader("Authorization").getOrElse("").split("\\s+") match {
      case Array("Basic", userAndPass) =>
        new String(Base64.getDecoder.decode(userAndPass), "UTF-8").split(":") match {
          case Array(user, password) => Option(user, password)
          case _ => None
        }
      case _ => None
    }
  }

  /**
    * ServiceCall that returns a list of allowed methods, all of which will also be listed as access controlled
    *
    * @param listOfOptions with the allowed options. Schema: "GET, POST, DELETE"
    *                      OPTIONS is added automatically
    */
  def allowedMethodsCustom(listOfOptions: String): ServiceCall[NotUsed, Done] = ServerServiceCall {
    (_, _) =>
      Future.successful {
        (ResponseHeader(200, MessageProtocol.empty, List(
          ("Allow", listOfOptions + ", OPTIONS"),
          ("Access-Control-Allow-Methods", listOfOptions + ", OPTIONS")
        )), Done)
      }
  }

}
