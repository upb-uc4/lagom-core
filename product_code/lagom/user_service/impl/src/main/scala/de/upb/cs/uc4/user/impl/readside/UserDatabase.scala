package de.upb.cs.uc4.user.impl.readside

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement}
import de.upb.cs.uc4.user.impl.actor.User
import de.upb.cs.uc4.user.impl.events.{OnUserCreate, OnUserDelete, UserEvent}
import de.upb.cs.uc4.user.model.Role

import scala.concurrent.{ExecutionContext, Future, Promise}

class UserDatabase(session: CassandraSession)(implicit ec: ExecutionContext) {

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

  /** Create empty courses table */
  def globalPrepare(): Future[Done] = {
    session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS students ( " +
        "username TEXT, PRIMARY KEY (username)) ;")
    session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS lecturers ( " +
        "username TEXT, PRIMARY KEY (username)) ;")
    session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS admins ( " +
        "username TEXT, PRIMARY KEY (username)) ;")
  }

  /** Finishes preparation of CQL Statements */
  def prepare(tag: AggregateEventTag[UserEvent]): Future[Done] = {
    val prepStudents = prepare("students", insertStudentPromise, deleteStudentPromise)
    val prepLecturers = prepare("lecturers", insertLecturerPromise, deleteLecturerPromise)
    val prepAdmins = prepare("admins", insertAdminPromise, deleteAdminPromise)

    //Combine all Futures to one future
    prepStudents.flatMap(_ => prepLecturers).flatMap(_ => prepAdmins)
  }

  private def prepare(table: String, insert: Promise[PreparedStatement], delete: Promise[PreparedStatement]): Future[Done] = {
    val prepInsert = session.prepare(s"INSERT INTO $table (username) VALUES (?) ;")
    insert.completeWith(prepInsert)

    val prepDelete = session.prepare(s"DELETE FROM $table WHERE username=? ;")
    delete.completeWith(prepDelete)

    prepInsert.flatMap(_ => prepDelete.map(_ => Done))
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

  private def bind(user: User) : PreparedStatement => List[BoundStatement] = {
    ps =>
      val bindWriteTitle = ps.bind()
      bindWriteTitle.setString("username", user.getUsername)
      List(bindWriteTitle)
  }
}
