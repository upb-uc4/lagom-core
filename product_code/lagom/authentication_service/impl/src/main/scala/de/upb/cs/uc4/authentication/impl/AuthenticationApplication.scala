package de.upb.cs.uc4.authentication.impl

import akka.Done
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.Hashing
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.JsonUsername
import de.upb.cs.uc4.user.model.user.AuthenticationUser
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

import scala.concurrent.Future
import scala.util.Random

abstract class AuthenticationApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with CORSComponents
    with AhcWSComponents {

  // Set HttpFilter to the default CorsFilter
  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[AuthenticationService](wire[AuthenticationServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = AuthenticationSerializerRegistry

  lazy val userService: UserService = serviceClient.implement[UserService]

  // Create empty user table if it doesn't exists
  cassandraSession.executeCreateTable(
    "CREATE TABLE IF NOT EXISTS authenticationTable ( " +
      "name TEXT, salt TEXT, password TEXT, role TEXT, PRIMARY KEY (name));")
    .onComplete(_ =>
      //Check if this table is empty
      cassandraSession.selectOne("SELECT * FROM authenticationTable;").onComplete(result =>
        if (result.isSuccess) {
          result.get match {
            //Insert default users
            case None =>
              createAccount("admin", "admin", AuthenticationRole.Admin)
              createAccount("student", "student", AuthenticationRole.Student)
              createAccount("lecturer", "lecturer", AuthenticationRole.Lecturer)
            case _ =>
          }
        }
      )
    )

  userService
    .userAuthenticationTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[AuthenticationUser, Future[Done]](msg => {
        createAccount(msg.username, msg.password, msg.role)
      }).mapAsync(8)(done => done)
    )

  userService
    .userDeletedTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[JsonUsername, Future[Done]](json => {
        deleteAccount(json.username)
      }).mapAsync(8)(done => done)
    )

  /** Adds an account to the authentication database
    *
    * @param name     is the name of the user
    * @param password is the password of the user
    * @param role     is the authentication role of the user
    */
  private def createAccount(name: String, password: String, role: AuthenticationRole): Future[Done] = {
    val salt = Random.alphanumeric.take(64).mkString
    cassandraSession.executeWrite(
      "INSERT INTO authenticationTable (name, salt, password, role) VALUES (?, ?, ?, ?);",
      Hashing.sha256(name),
      salt,
      Hashing.sha256(salt + password),
      role.toString
    )
  }

  /** Deletes a user from the authentication database
    *
    * @param username is the name of the user
    */
  private def deleteAccount(username: String): Future[Done] = {
    cassandraSession.executeWrite("DELETE FROM authenticationTable WHERE name=? ;", Hashing.sha256(username))
  }
}


