package de.upb.cs.uc4.user.impl.readside

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.impl.actor.UserState
import de.upb.cs.uc4.user.impl.commands.{CreateUser, UserCommand}
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.user._
import de.upb.cs.uc4.user.model.{Address, Role}
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class UserDatabase(database: Database, clusterSharding: ClusterSharding)(implicit ec: ExecutionContext) {

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** Table definition of a user table */
  abstract class UserTable(tag: Tag, name: String) extends Table[String](tag, s"uc4${name}Table") {
    def username: Rep[String] = column[String]("username", O.PrimaryKey)

    override def * : ProvenShape[String] = username <>
      (username => username, (username: String) => Some(username))
  }

  // Specific tables
  class AdminTable(tag: Tag) extends UserTable(tag, "Admin")
  class LecturerTable(tag: Tag) extends UserTable(tag, "Lecturer")
  class StudentTable(tag: Tag) extends UserTable(tag, "Student")

  val admins = TableQuery[AdminTable]
  val lecturers = TableQuery[LecturerTable]
  val students = TableQuery[StudentTable]

  /** Creates all needed tables for the different roles */
  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] = {
    admins.schema.createIfNotExists >> //AND THEN
    lecturers.schema.createIfNotExists >> //AND THEN
    students.schema.createIfNotExists.andFinally(DBIO.successful{
      //Add default users
      val address: Address = Address("Deppenstraße", "42a", "1337", "Entenhausen", "Nimmerland")
      val student: User = Student("student", Role.Student, address, "firstName", "LastName", "Picture", "example@mail.de", "1990-12-11", "IN", "421769", 9000, List())
      val lecturer: User = Lecturer("lecturer", Role.Lecturer, address, "firstName", "LastName", "Picture", "example@mail.de", "1991-12-11", "Ich bin bloed", "Genderstudies")
      val admin: User = Admin("admin", Role.Admin, address, "firstName", "LastName", "Picture", "example@mail.de", "1992-12-10")

      addUser(student, AuthenticationUser("student", "student", AuthenticationRole.Student))
      addUser(lecturer, AuthenticationUser("lecturer", "lecturer", AuthenticationRole.Lecturer))
      addUser(admin, AuthenticationUser("admin", "admin", AuthenticationRole.Admin))
    })
  }

  /** helper method to add a user */
  private def addUser(user: User, authenticationUser: AuthenticationUser) =
    getAll(user.role).map{ result =>
      if(result.isEmpty){
        entityRef(user.username).ask[Confirmation](replyTo => CreateUser(user, authenticationUser, replyTo))
      }
    }

  /** Returns a Sequence of all user with
   * the specific role
   *
   * @param role of the users
   */
  def getAll(role: Role): Future[Seq[String]] =
    database.run(findAllQuery(role))

  /** Adds a user to the matching table
   *
   * @param user which should get added
   */
  def addUser(user: User): DBIO[Done] = {
    val table = getTable(user.role)
    findByUsernameQuery(user.username, table)
      .flatMap {
        case None => table += user.username
        case _    => DBIO.successful(Done)
      }
      .map(_ => Done)
      .transactionally
  }

  /** Deletes a user from the matching table
   *
   * @param user which should get removed
   */
  def removeUser(user: User): DBIO[Done] = {
    getTable(user.role)
      .filter(_.username === user.username)
      .delete
      .map(_ => Done)
      .transactionally
  }

  /** Returns the table for the specific role
   *
   * @param role to specify the table
   */
  private def getTable(role: Role): TableQuery[_ <: UserTable] =
    role match {
      case Role.Admin => admins
      case Role.Lecturer => lecturers
      case Role.Student => students
    }

  /** Returns the query to get all User
   * of the specific role
   *
   * @param role to specify
   */
  private def findAllQuery(role: Role): DBIO[Seq[String]] = getTable(role).result

  /** Returns the query to find a user by his username
   *
   * @param username of the user
   * @param table on which the query is executed
   */
  private def findByUsernameQuery(username: String, table: TableQuery[_ <: UserTable]): DBIO[Option[String]] =
    table
      .filter(_.username === username)
      .result
      .headOption
}
