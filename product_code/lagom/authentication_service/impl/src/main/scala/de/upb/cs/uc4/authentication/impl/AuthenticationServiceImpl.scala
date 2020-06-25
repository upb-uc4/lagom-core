package de.upb.cs.uc4.authentication.impl

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{AuthenticationResponse, AuthenticationRole}
import de.upb.cs.uc4.authentication.model.AuthenticationResponse.AuthenticationResponse
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.Hashing

import scala.concurrent.ExecutionContext

class AuthenticationServiceImpl(cassandraSession: CassandraSession)
                               (implicit ec: ExecutionContext) extends AuthenticationService {

  /** @inheritdoc */
  override def check(username: String, password: String): ServiceCall[Seq[AuthenticationRole], AuthenticationResponse]
  = ServiceCall {
    roles =>
      cassandraSession.selectOne("SELECT * FROM authenticationTable WHERE name=? ;", Hashing.sha256(username))
        .map {
          case Some(row) =>
            val salt = row.getString("salt")

            if (row.getString("password") != Hashing.sha256(salt + password)) {
              AuthenticationResponse.WrongPassword
            } else {
              if (!roles.map(_.toString).contains(row.getString("role"))) {
                AuthenticationResponse.NotAuthorized
              } else {
                AuthenticationResponse.Correct
              }
            }

          case None => AuthenticationResponse.WrongUsername
        }
  }

  /** Returns role of the given user */
  override def getRole(username: String): ServiceCall[NotUsed, AuthenticationRole] = ServiceCall{ _ =>
    cassandraSession.selectOne("SELECT * FROM authenticationTable WHERE name=? ;", Hashing.sha256(username))
      .map {
        case Some(row) => AuthenticationRole.withName(row.getString("role"))
        case None => throw NotFound("User does not exists.")
      }
  }
}
