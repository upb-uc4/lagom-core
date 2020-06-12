package de.upb.cs.uc4.shared

import java.util.Base64

import com.lightbend.lagom.scaladsl.api.transport.{Forbidden, NotFound, RequestHeader, TransportErrorCode, ExceptionMessage}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.user.model.Role.Role
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.varargs
import scala.concurrent.ExecutionContext

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
  def authenticated[Request, Response](role: Role*)(serviceCall: ServerServiceCall[Request, Response])
                                      (implicit auth: AuthenticationService, ec: ExecutionContext)
  : ServerServiceCall[Request, Response] = {
    ServerServiceCall.composeAsync[Request, Response] { requestHeader =>
      val userPw = getUserAndPassword(requestHeader)

      if(userPw.isEmpty){
        throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"), new ExceptionMessage("Unauthorized", "No Authorization given"))
      }

      val (user, pw) = userPw.get

      auth.check(user, pw).invoke(role).map{
        case AuthenticationResponse.Correct => serviceCall
        case AuthenticationResponse.WrongUsername => throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"), new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
        case AuthenticationResponse.WrongPassword => throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"), new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
        case AuthenticationResponse.NotAuthorized => throw Forbidden("Not enough privileges for this call.")
      }
    }
  }

  /**
    * Reads username and password out of the header
    *
    * @param requestHeader with the an authentication header
    * @return an Option with a String tuple
    */
  def getUserAndPassword(requestHeader: RequestHeader): Option[(String, String)] ={
    requestHeader.getHeader("Authorization").getOrElse("").split("\\s+") match {
      case Array("Basic", userAndPass) =>
        new String(Base64.getDecoder.decode(userAndPass), "UTF-8").split(":")match {
          case Array(user, password) => Option(user, password)
          case _                     => None
        }
      case _ => None
    }
  }
}
