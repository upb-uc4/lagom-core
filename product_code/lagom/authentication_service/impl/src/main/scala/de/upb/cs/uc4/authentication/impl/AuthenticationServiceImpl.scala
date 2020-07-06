package de.upb.cs.uc4.authentication.impl

import java.time.Clock

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.server.Hashing

import scala.concurrent.ExecutionContext

class AuthenticationServiceImpl(cassandraSession: CassandraSession)
                               (implicit ec: ExecutionContext) extends AuthenticationService {

  private implicit val clock: Clock = Clock.systemUTC

  /** @inheritdoc */
  override def check(username: String, password: String): ServiceCall[NotUsed, (String, AuthenticationRole)] = ServiceCall { _ =>
    cassandraSession.selectOne("SELECT * FROM authenticationTable WHERE name=? ;", Hashing.sha256(username))
      .flatMap {
        case Some(row) =>
          val salt = row.getString("salt")

          if (row.getString("password") != Hashing.sha256(salt + password)) {
            throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"),
              new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
          } else {
            getRole(username).invoke().map { role =>
              (username, role)
            }
          }

        case None =>
          throw new Forbidden(TransportErrorCode(401, 1003, "Password Error, wrong password"),
            new ExceptionMessage("Unauthorized", "Username and password combination does not exist"))
      }
  }

  /** Returns role of the given user */
  override def getRole(username: String): ServiceCall[NotUsed, AuthenticationRole] = ServiceCall { _ =>
    cassandraSession.selectOne("SELECT * FROM authenticationTable WHERE name=? ;", Hashing.sha256(username))
      .map {
        case Some(row) => AuthenticationRole.withName(row.getString("role"))
        case None => throw NotFound("Username does not exists.")
      }
  }
}
