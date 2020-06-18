package de.upb.cs.uc4.authentication.impl

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.authentication.model.AuthenticationResponse.AuthenticationResponse
import de.upb.cs.uc4.shared.Hashing
import de.upb.cs.uc4.shared.ServiceCallFactory._
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.{JsonRole, Role, User}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class AuthenticationServiceImpl(cassandraSession: CassandraSession)
                               (implicit ec: ExecutionContext) extends AuthenticationService {

  /** @inheritdoc */
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

  def getRole(username: String): ServerServiceCall[NotUsed, JsonRole] = authenticated[NotUsed, JsonRole](Role.Student, Role.Lecturer, Role.Admin){ServerServiceCall{
      (requestHeader, _) =>
        var usernameFromHeader = getUserAndPassword(requestHeader) match {
          case Some(usernamePassword) => usernamePassword._1
          case _ => throw new Forbidden(TransportErrorCode(500, 1003, "Internal Server Error"), new ExceptionMessage("Internal Server Error", "Failed to retrieve Username"))
        }
        cassandraSession.selectOne("SELECT role FROM authenticationTable WHERE name=? ;", Hashing.sha256(usernameFromHeader)).map{
          case Some(row) => Role.withName(row.getString("role"))
          case _ => throw new Forbidden(TransportErrorCode(500, 1003, "Internal Server Error"), new ExceptionMessage("Internal Server Error", "Failed to retrieve Username"))
        }.flatMap{
          case Role.Admin  =>
            if(usernameFromHeader == username){
              Future.successful(ResponseHeader(200, MessageProtocol.empty, List(("1","Operation Successful"))), JsonRole(Role.Admin))
            }else{
              cassandraSession.selectOne("SELECT role FROM authenticationTable WHERE name=? ;", Hashing.sha256(username)).map{
                case Some(row) => (ResponseHeader(200, MessageProtocol.empty, List(("1","Operation Successful"))), JsonRole(Role.withName(row.getString("role"))))
                case _ => throw new Forbidden(TransportErrorCode(404, 1003, "Not Found"), new ExceptionMessage("Not Found", "Username not found"))
              }
            }
          case userRole =>
            if(usernameFromHeader == username){
              Future.successful(ResponseHeader(200, MessageProtocol.empty, List(("1","Operation Successful"))), JsonRole(userRole))
            }
            else {
              throw new Forbidden(TransportErrorCode(403, 1003, "Bad Request"), new ExceptionMessage("Forbidden", "Not enough privileges for this action."))
            }
        }
    }
  }(this, ec)

  /** @inheritdoc */
  override def set(): ServiceCall[User, Done] = authenticated[User, Done](Role.Admin) { user =>
    val salt = Random.alphanumeric.take(64).mkString //Generates random salt with 64 characters
    cassandraSession.executeWrite(
      "INSERT INTO authenticationTable (name, salt, password, role) VALUES (?, ?, ?, ?);",
      Hashing.sha256(user.username),
      salt,
      Hashing.sha256(salt + user.password),
      user.role.toString
    )
  }(this, ec)

  /** @inheritdoc */
  override def delete(username: String): ServiceCall[NotUsed, Done] = authenticated[NotUsed, Done](Role.Admin) { _ =>
    cassandraSession.executeWrite("DELETE FROM authenticationTable WHERE name=? ;", Hashing.sha256(username))
  }(this, ec)

  /** @inheritdoc */
  override def options(): ServiceCall[NotUsed, Done] = ServerServiceCall {
    (_, _) =>
      Future.successful {
        (ResponseHeader(200, MessageProtocol.empty, List(
          ("Allow", "POST, OPTIONS, DELETE"),
          ("Access-Control-Allow-Methods", "POST, OPTIONS, DELETE")
        )), Done)
      }
  }

  /** @inheritdoc */
  override def optionsGet(): ServiceCall[NotUsed, Done] = ServerServiceCall {
    (_, _) =>
      Future.successful {
        (ResponseHeader(200, MessageProtocol.empty, List(
          ("Allow", "GET, OPTIONS"),
          ("Access-Control-Allow-Methods", "GET, OPTIONS")
        )), Done)
      }
  }

  /** Allows POST */
  def allowedMethodsPOST(): ServiceCall[NotUsed, Done] = de.upb.cs.uc4.shared.ServiceCallFactory.allowedMethodsCustom("POST")

  /** Allows GET */
  def allowedMethodsGET(): ServiceCall[NotUsed, Done] = de.upb.cs.uc4.shared.ServiceCallFactory.allowedMethodsCustom("GET")

  /** Allows DELETE */
  def allowedMethodsDELETE(): ServiceCall[NotUsed, Done] = de.upb.cs.uc4.shared.ServiceCallFactory.allowedMethodsCustom("DELETE")


}
