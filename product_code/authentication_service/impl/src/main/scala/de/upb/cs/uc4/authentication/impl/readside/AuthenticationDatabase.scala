package de.upb.cs.uc4.authentication.impl.readside

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import de.upb.cs.uc4.authentication.impl.actor.AuthenticationState
import de.upb.cs.uc4.authentication.impl.commands.{ AuthenticationCommand, SetAuthentication }
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser }
import de.upb.cs.uc4.shared.server.Hashing
import de.upb.cs.uc4.shared.server.messages.Confirmation
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class AuthenticationDatabase(database: Database, clusterSharding: ClusterSharding)(implicit ec: ExecutionContext) {

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[AuthenticationCommand] =
    clusterSharding.entityRefFor(AuthenticationState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** Table definition of an authentication table */
  class AuthenticationTable(tag: Tag) extends Table[String](tag, "uc4AuthenticationTable") {
    def username: Rep[String] = column[String]("username", O.PrimaryKey)

    override def * : ProvenShape[String] = username <>
      (username => username, (username: String) => Some(username))
  }

  val authenticationUsers = TableQuery[AuthenticationTable]

  /** Creates needed table */
  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] =
    authenticationUsers.schema.createIfNotExists.andFinally(DBIO.successful {
      getAll.map { result =>
        if (result.isEmpty) {
          val student = AuthenticationUser("student", "student", AuthenticationRole.Student)
          val lecturer = AuthenticationUser("lecturer", "lecturer", AuthenticationRole.Lecturer)
          val admin = AuthenticationUser("admin", "admin", AuthenticationRole.Admin)

          entityRef(Hashing.sha256(student.username)).ask[Confirmation](replyTo => SetAuthentication(student, replyTo))
          entityRef(Hashing.sha256(lecturer.username)).ask[Confirmation](replyTo => SetAuthentication(lecturer, replyTo))
          entityRef(Hashing.sha256(admin.username)).ask[Confirmation](replyTo => SetAuthentication(admin, replyTo))
        }
      }
    })

  /** Returns a Sequence of all hashed usernames */
  def getAll: Future[Seq[String]] = database.run(findAllQuery)

  /** Adds an AuthenticationUser to the table
    *
    * @param user which should get added
    */
  def addAuthenticationUser(user: AuthenticationUser): DBIO[Done] =
    findByUsernameQuery(user.username)
      .flatMap {
        case None => authenticationUsers += Hashing.sha256(user.username)
        case _    => DBIO.successful(Done)
      }
      .map(_ => Done)
      .transactionally

  /** Deletes an AuthenticationUser from the table
    *
    * @param username of the user which should get removed
    */
  def removeAuthenticationUser(username: String): DBIO[Done] =
    authenticationUsers
      .filter(_.username === username)
      .delete
      .map(_ => Done)
      .transactionally

  /** Returns the query to get all hashed usernames */
  private def findAllQuery: DBIO[Seq[String]] = authenticationUsers.result

  /** Returns the query to find a user by his username */
  private def findByUsernameQuery(username: String): DBIO[Option[String]] =
    authenticationUsers
      .filter(_.username === Hashing.sha256(username))
      .result
      .headOption

}
