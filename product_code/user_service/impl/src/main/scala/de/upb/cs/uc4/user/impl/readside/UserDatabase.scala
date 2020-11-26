package de.upb.cs.uc4.user.impl.readside

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.impl.actor.UserState
import de.upb.cs.uc4.user.impl.commands.{ CreateUser, UserCommand }
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.user._
import de.upb.cs.uc4.user.model.{ Address, Role }
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.{ ExecutionContext, Future }

class UserDatabase(database: Database, clusterSharding: ClusterSharding)(implicit ec: ExecutionContext, timeout: Timeout) {

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserState.typeKey, id)

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

  /** Entry definition of the image table */
  case class ImageEntry(username: String, image: Array[Byte], thumbnail: Array[Byte])

  /** Table definition of the image table */
  class ImageTable(tag: Tag) extends Table[ImageEntry](tag, s"uc4ImageTable") {
    def username: Rep[String] = column[String]("username", O.PrimaryKey)

    def image: Rep[Array[Byte]] = column[Array[Byte]]("image")

    def thumbnail: Rep[Array[Byte]] = column[Array[Byte]]("thumbnail")

    override def * : ProvenShape[ImageEntry] =
      (username, image, thumbnail) <> ((ImageEntry.apply _).tupled, ImageEntry.unapply)
  }

  val admins = TableQuery[AdminTable]
  val lecturers = TableQuery[LecturerTable]
  val students = TableQuery[StudentTable]
  val images = TableQuery[ImageTable]

  /** Creates all needed tables for the different roles */
  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] = {
    images.schema.createIfNotExists >> //AND THEN
      admins.schema.createIfNotExists >> //AND THEN
      lecturers.schema.createIfNotExists >> //AND THEN
      students.schema.createIfNotExists.andFinally(DBIO.successful {
        //Add default users
        val address: Address = Address("Gänseweg", "42a", "13337", "Entenhausen", "Germany")
        val student: User = Student("student", "c3R1ZGVudHN0dWRlbnQ=", isActive = true, Role.Student, address, "Stu", "Dent", "student@mail.de", "+49123456789", "1990-12-11", "", "7421769")
        val lecturer: User = Lecturer("lecturer", "bGVjdHVyZXJsZWN0dXJlcg==", isActive = true, Role.Lecturer, address, "Lect", "Urer", "lecturer@mail.de", "+49123456789", "1991-12-11", "Heute kommt der kleine Gauss dran.", "Mathematics")
        val admin: User = Admin("admin", "YWRtaW5hZG1pbg==", isActive = true, Role.Admin, address, "Ad", "Min", "admin@mail.de", "+49123456789", "1992-12-10")

        addDefaultUser(student, "governmentIdStudent")
        addDefaultUser(lecturer, "governmentIdLecturer")
        addDefaultUser(admin, "governmentIdAdmin")
      })
  }

  /** helper method to add a user during table creation. */
  private def addDefaultUser(user: User, governmentId: String) =
    getAll(user.role).map { result =>
      if (result.isEmpty) {
        entityRef(user.username).ask[Confirmation](replyTo => CreateUser(user, governmentId, replyTo))
      }
    }

  /** Returns a Sequence of all users with
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

  /** Returns an option of the image of a user
    *
    * @param username of the owner of the image
    */
  def getImage(username: String): Future[Option[Array[Byte]]] =
    database.run(findImageByUsernameQuery(username))

  /** Returns an option of the thumbnail of a user
    *
    * @param username of the owner of the image
    */
  def getThumbnail(username: String): Future[Option[Array[Byte]]] =
    database.run(findThumbnailByUsernameQuery(username))

  /** Creates or updates the image of a user
    *
    * @param username of the owner of the image
    * @param image as byte array
    */
  def setImage(username: String, image: Array[Byte], thumbnail: Array[Byte]): Future[Done] =
    database.run(setImageQuery(username, image, thumbnail))

  /** Deletes an image of a user
    *
    * @param username owner of the image
    */
  def deleteImage(username: String): Future[Done] =
    database.run(deleteImageQuery(username))

  /** Returns the table for the specific role
    *
    * @param role to specify the table
    */
  private def getTable(role: Role): TableQuery[_ <: UserTable] =
    role match {
      case Role.Admin    => admins
      case Role.Lecturer => lecturers
      case Role.Student  => students
    }

  /** Returns the query to get all users
    * of the specific role
    *
    * @param role to specify
    */
  private def findAllQuery(role: Role): DBIO[Seq[String]] = getTable(role).result

  /** Returns the query to find a user by his username
    *
    * @param username of the user
    * @param table    on which the query is executed
    */
  private def findByUsernameQuery(username: String, table: TableQuery[_ <: UserTable]): DBIO[Option[String]] =
    table
      .filter(_.username === username)
      .result
      .headOption

  /** Returns the query to find an image by a username
    *
    * @param username of the owner of the image
    */
  private def findImageByUsernameQuery(username: String): DBIO[Option[Array[Byte]]] =
    images
      .filter(_.username === username)
      .result
      .headOption
      .map(_.map(entry => entry.image))

  /** Returns the query to find an thumbnail by a username
    *
    * @param username of the owner of the image
    */
  private def findThumbnailByUsernameQuery(username: String): DBIO[Option[Array[Byte]]] =
    images
      .filter(_.username === username)
      .result
      .headOption
      .map(_.map(entry => entry.thumbnail))

  /** Returns the query to create or update the image of a user
    *
    * @param username of the owner of the image
    * @param image as byte array
    */
  private def setImageQuery(username: String, image: Array[Byte], thumbnail: Array[Byte]): DBIO[Done] =
    images
      .insertOrUpdate(ImageEntry(username, image, thumbnail))
      .map(_ => Done)
      .transactionally

  /** Returns the query to delete an image of a user
    *
    * @param username owner of the image
    */
  def deleteImageQuery(username: String): DBIO[Done] =
    images
      .filter(_.username === username)
      .delete
      .map(_ => Done)
      .transactionally
}
