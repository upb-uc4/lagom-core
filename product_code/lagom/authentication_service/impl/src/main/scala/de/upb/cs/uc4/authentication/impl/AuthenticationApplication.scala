package de.upb.cs.uc4.authentication.impl

import akka.Done
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.shared.Hashing
import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.Role.Role
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

  // Create empty user table if it doesn't exists
  cassandraSession.executeCreateTable(
    "CREATE TABLE IF NOT EXISTS authenticationTable ( " +
      "name TEXT, salt TEXT, password TEXT, role TEXT, PRIMARY KEY (name));")
    .onComplete(_ =>
      //Check if this table is empty
      cassandraSession.selectOne("SELECT * FROM authenticationTable;").onComplete(result =>
        if(result.isSuccess) {
          result.get match {
            //Insert default users
            case None =>
              createAccount("admin", Role.Admin)
              createAccount("student", Role.Student)
              createAccount("lecturer", Role.Lecturer)
            case _ =>
          }
        }
      )
    )

  /** Adds an account to the authentication database
    *
    * @param name is the name and password of the user
    * @param role is the authentication role of the user
    */
  private def createAccount(name: String, role: Role): Future[Done] = {
    val salt = Random.alphanumeric.take(64).mkString
    cassandraSession.executeWrite(
      "INSERT INTO authenticationTable (name, salt, password, role) VALUES (?, ?, ?, ?);",
      Hashing.sha256(name),
      salt,
      Hashing.sha256(salt + name),
      role.toString
    )
  }
}


