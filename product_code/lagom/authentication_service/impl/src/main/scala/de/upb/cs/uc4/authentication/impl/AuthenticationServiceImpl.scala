package de.upb.cs.uc4.authentication.impl

import java.time.Clock
import java.util.{Base64, Calendar}

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.Hashing
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import javax.inject.Inject
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

class AuthenticationServiceImpl @Inject()(cassandraSession: CassandraSession, config: Configuration)
                                         (implicit ec: ExecutionContext) extends AuthenticationService {

  private implicit val clock: Clock = Clock.systemUTC

  /** @inheritdoc */
  override def check(jws: String): ServiceCall[NotUsed, (String, AuthenticationRole)] = ServiceCall { _ =>
    val key = config.get[String]("play.http.secret.key")
    try {
      val claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jws).getBody
      Future.successful(
        claims.get("username", classOf[String]),
        AuthenticationRole.withName(claims.get("AuthenticationRole", classOf[String]))
      )
    } catch {
      case _: Exception =>
        throw new Forbidden(TransportErrorCode(401, 1003, "Signature Error"),
          new ExceptionMessage("Unauthorized", "Jwts is not valid"))
    }

  }

  /** Returns role of the given user */
  override def getRole(username: String): ServiceCall[NotUsed, AuthenticationRole] = ServiceCall { _ =>
    cassandraSession.selectOne("SELECT * FROM authenticationTable WHERE name=? ;", Hashing.sha256(username))
      .map {
        case Some(row) => AuthenticationRole.withName(row.getString("role"))
        case None => throw NotFound("User does not exists.")
      }
  }

  /** Logs the user in */
  override def login(): ServiceCall[NotUsed, String] = ServerServiceCall { (header, _) =>
    val userPw = getUserAndPassword(header)

    if (userPw.isEmpty) {
      throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"),
        new ExceptionMessage("Unauthorized", "No Authorization given"))
    }

    val (user, pw) = userPw.get

    cassandraSession.selectOne("SELECT * FROM authenticationTable WHERE name=? ;", Hashing.sha256(user))
      .flatMap {
        case Some(row) =>
          val salt = row.getString("salt")

          if (row.getString("password") != Hashing.sha256(salt + pw)) {
            throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"),
              new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
          } else {
            getRole(user).invoke().map { role =>
              val key = config.get[String]("play.http.secret.key")
              val now = Calendar.getInstance()
              now.add(Calendar.MINUTE, config.get[Int]("uc4.authentication.logout"))
              val jws =
                Jwts.builder()
                  .setSubject("authentication")
                  .setExpiration(now.getTime)
                  .claim("username", user)
                  .claim("AuthenticationRole", role.toString)
                  .signWith(SignatureAlgorithm.HS256, key)
                  .compact()

              (ResponseHeader(200, MessageProtocol.empty, List(("1", "Login successful"))), jws)
            }
          }

        case None =>
          throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"),
            new ExceptionMessage("Unauthorized", "No Authorization given"))
      }

  }

  /** Logs the user out */
  override def logout(): ServiceCall[NotUsed, Done] = ???

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
}
