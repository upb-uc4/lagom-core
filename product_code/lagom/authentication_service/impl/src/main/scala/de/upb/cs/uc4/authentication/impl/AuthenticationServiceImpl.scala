package de.upb.cs.uc4.authentication.impl

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{MessageProtocol, ResponseHeader}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.authentication.model.AuthenticationResponse.AuthenticationResponse
import de.upb.cs.uc4.shared.Hashing
import de.upb.cs.uc4.shared.ServiceCallFactory._
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.{Role, User}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class AuthenticationServiceImpl(cassandraSession: CassandraSession)
                               (implicit ec: ExecutionContext) extends AuthenticationService {

  override def check(username: String, password: String): ServiceCall[Seq[Role], AuthenticationResponse] = ServiceCall {
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

  override def set(): ServiceCall[User, Done] = authenticated[User, Done](Role.Admin) { user =>
    val salt = Random.alphanumeric.take(64).mkString
    cassandraSession.executeWrite(
      "INSERT INTO authenticationTable (name, salt, password, role) VALUES (?, ?, ?, ?);",
      Hashing.sha256(user.username),
      salt,
      Hashing.sha256(salt + user.password),
      user.role.toString
    )
  }(this, ec)

  override def delete(username: String): ServiceCall[NotUsed, Done] = authenticated[NotUsed, Done](Role.Admin) { _ =>
    cassandraSession.executeWrite("DELETE FROM authenticationTable WHERE name=? ;", Hashing.sha256(username))
  }(this, ec)

  override def options(): ServiceCall[NotUsed, Done] = ServerServiceCall {
    (_, _) =>
      Future.successful {
        (ResponseHeader(200, MessageProtocol.empty, List(
          ("Allow", "POST, OPTIONS, DELETE"),
          ("Access-Control-Allow-Methods", "POST, OPTIONS, DELETE")
        )), Done)
      }
  }
}
