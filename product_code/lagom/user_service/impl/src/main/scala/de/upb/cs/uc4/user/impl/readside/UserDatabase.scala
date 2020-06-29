package de.upb.cs.uc4.user.impl.readside

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement}
import de.upb.cs.uc4.shared.messages.Confirmation
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.{User, UserState}
import de.upb.cs.uc4.user.impl.events.{OnUserCreate, OnUserDelete, UserEvent}
import de.upb.cs.uc4.user.model.{Address, Role}
import de.upb.cs.uc4.user.impl.UserApplication
import de.upb.cs.uc4.user.impl.commands.{CreateUser, UserCommand}
import de.upb.cs.uc4.user.model.post.PostMessageStudent
import de.upb.cs.uc4.user.model.user.{Admin, AuthenticationUser, Lecturer, Student}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.util.Try

class UserDatabase(session: CassandraSession, clusterSharding: ClusterSharding)(implicit ec: ExecutionContext) {

  implicit val timeout: Timeout = Timeout(5.seconds)

  //Prepared CQL Statements
  private val insertStudentPromise = Promise[PreparedStatement] // initialized in prepare
  private def insertStudent(): Future[PreparedStatement] = insertStudentPromise.future

  private val deleteStudentPromise = Promise[PreparedStatement] // initialized in prepare
  private def deleteStudent(): Future[PreparedStatement] = deleteStudentPromise.future

  private val insertLecturerPromise = Promise[PreparedStatement] // initialized in prepare
  private def insertLecturer(): Future[PreparedStatement] = insertLecturerPromise.future

  private val deleteLecturerPromise = Promise[PreparedStatement] // initialized in prepare
  private def deleteLecturer(): Future[PreparedStatement] = deleteLecturerPromise.future

  private val insertAdminPromise = Promise[PreparedStatement] // initialized in prepare
  private def insertAdmin(): Future[PreparedStatement] = insertAdminPromise.future

  private val deleteAdminPromise = Promise[PreparedStatement] // initialized in prepare
  private def deleteAdmin(): Future[PreparedStatement] = deleteAdminPromise.future

  private def entityRef(id: String): EntityRef[UserCommand] = clusterSharding.entityRefFor(UserState.typeKey, id)



  private def addUserOnCreation(user: User, authenticationUser: AuthenticationUser, table: String): Try[_] => _ = {
    _ => //Check if this table is empty

      session.selectOne(s"SELECT * FROM $table;").onComplete(result =>
        if (result.isSuccess) {
          result.get match {
            //Insert default users
            case None =>
              entityRef(user.getUsername).ask[Confirmation](replyTo => CreateUser(user,authenticationUser,replyTo))
            case _ =>
          }
        }
      )
  }

  /** Create empty tables for admins, users and lecturers */
  def globalPrepare(): Future[Done] = {
    val address: Address = Address("Deppenstraße", "42a", "1337", "Entenhausen", "Nimmerland")
    val student : User = User(Student("student", Role.Student, address, "firstName", "LastName", "Picture", "example@mail.de", "IN", "421769", 9000, List()))
    val lecturer: User = User(Lecturer("lecturer", Role.Lecturer, address, "firstName", "LastName", "Picture", "example@mail.de", "Ich bin bloed", "Genderstudies"))
    val admin: User = User(Admin("admin", Role.Admin, address, "firstName", "LastName", "Picture", "example@mail.de"))
    val students = session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS students ( " +
        "username TEXT, PRIMARY KEY (username)) ;")
    students.onComplete(addUserOnCreation(student, AuthenticationUser("student", "student", Role.Student), "students"))

    val lecturers = session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS lecturers ( " +
        "username TEXT, PRIMARY KEY (username)) ;")
    lecturers.onComplete(addUserOnCreation(lecturer, AuthenticationUser("lecturer", "lecturer", Role.Lecturer), "lecturers"))

    val admins = session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS admins ( " +
        "username TEXT, PRIMARY KEY (username)) ;")
    admins.onComplete(addUserOnCreation(admin, AuthenticationUser("admin", "admin", Role.Admin), "admins"))

    //This comprehension waits for every future to be completed and then yields Done
    for {
      _ <- students
      _ <- lecturers
      _ <- admins
    } yield Done
  }

  /** Finishes preparation of CQL Statements */
  def prepare(tag: AggregateEventTag[UserEvent]): Future[Done] = {
    val prepStudents = prepare("students", insertStudentPromise, deleteStudentPromise)
    val prepLecturers = prepare("lecturers", insertLecturerPromise, deleteLecturerPromise)
    val prepAdmins = prepare("admins", insertAdminPromise, deleteAdminPromise)

    //This comprehension waits for every future to be completed and then yields Done
    for {
      _ <- prepStudents
      _ <- prepLecturers
      _ <- prepAdmins
    } yield Done
  }

  /** Helper method that prepares an INSERT and DELETE statement for the given table.
    *
    * @param table  name of the table the statements should be prepared for
    * @param insert the insert promise
    * @param delete the delete promise
    * @return Done when preparation of both statements is finished
    */
  private def prepare(table: String, insert: Promise[PreparedStatement], delete: Promise[PreparedStatement]): Future[Done] = {
    val prepInsert = session.prepare(s"INSERT INTO $table (username) VALUES (?) ;")
    insert.completeWith(prepInsert)

    val prepDelete = session.prepare(s"DELETE FROM $table WHERE username=? ;")
    delete.completeWith(prepDelete)

    //This comprehension waits for every future to be completed and then yields Done
    for {
      _ <- prepInsert
      _ <- prepDelete
    } yield Done
  }

  /** EventHandler for OnUserCreate event */
  def addUser(eventElement: EventStreamElement[OnUserCreate]): Future[List[BoundStatement]] = {
    eventElement.event.user.role match {
      case Role.Student =>
        insertStudent().map(bind(eventElement.event.user))
      case Role.Lecturer =>
        insertLecturer().map(bind(eventElement.event.user))
      case Role.Admin =>
        insertAdmin().map(bind(eventElement.event.user))
    }
  }

  /** EventHandler for OnUserDelete event */
  def deleteUser(eventElement: EventStreamElement[OnUserDelete]): Future[List[BoundStatement]] = {
    eventElement.event.user.role match {
      case Role.Student =>
        deleteStudent().map(bind(eventElement.event.user))
      case Role.Lecturer =>
        deleteLecturer().map(bind(eventElement.event.user))
      case Role.Admin =>
        deleteAdmin().map(bind(eventElement.event.user))
    }
  }

  /** returns helper method to bind statements */
  private def bind(user: User): PreparedStatement => List[BoundStatement] = {
    ps =>
      val bindWriteTitle = ps.bind()
      bindWriteTitle.setString("username", user.getUsername)
      List(bindWriteTitle)
  }
}
