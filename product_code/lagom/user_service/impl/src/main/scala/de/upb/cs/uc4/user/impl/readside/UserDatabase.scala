package de.upb.cs.uc4.user.impl.readside

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement}
import de.upb.cs.uc4.user.impl.events.UserEvent

import scala.concurrent.{ExecutionContext, Future, Promise}

class UserDatabase(session: CassandraSession)(implicit ec: ExecutionContext) {

  //Prepared CQL Statements
  private val insertUserPromise                       = Promise[PreparedStatement] // initialized in prepare
  private def insertUser(): Future[PreparedStatement] = insertUserPromise.future
  private val deleteUserPromise                       = Promise[PreparedStatement] // initialized in prepare
  private def deleteUser(): Future[PreparedStatement] = deleteUserPromise.future

  /** Create empty courses table */
  def globalPrepare(): Future[Done] = {
    session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS users ( " +
        "username TEXT, PRIMARY KEY (username)) ;")
  }

  /** Finishes preparation of CQL Statements */
  def prepare(tag: AggregateEventTag[UserEvent]): Future[Done] = {
    val prepInsert = session.prepare("INSERT INTO users (username) VALUES (?) ;")
    insertUserPromise.completeWith(prepInsert)

    val prepDelete = session.prepare("DELETE FROM users WHERE username=? ;")
    deleteUserPromise.completeWith(prepDelete)

    prepInsert.flatMap(_ => prepDelete.map(_ => Done))
  }

  /** EventHandler for OnCourseCreate event */
  def addUser(eventElement: EventStreamElement[OnCourseCreate]): Future[List[BoundStatement]] = {
    insertUser().map { ps =>
      val bindWriteTitle = ps.bind()
      bindWriteTitle.setString("username", eventElement.event.course.courseId)
      List(bindWriteTitle)
    }
  }

  /** EventHandler for OnCourseDelete event */
  def deleteUser(eventElement: EventStreamElement[OnCourseDelete]): Future[List[BoundStatement]] = {
    deleteUser().map { ps =>
      val bindWriteTitle = ps.bind()
      bindWriteTitle.setString("username", eventElement.event.id)
      List(bindWriteTitle)
    }
  }
}
