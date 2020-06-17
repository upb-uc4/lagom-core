package de.upb.cs.uc4.course.impl.readside

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement}
import de.upb.cs.uc4.course.impl.events.{CourseEvent, OnCourseCreate, OnCourseDelete}

import scala.concurrent.{ExecutionContext, Future, Promise}

class CourseDatabase(session: CassandraSession)(implicit ec: ExecutionContext) {

  //Prepared CQL Statements
  private val insertCoursePromise                       = Promise[PreparedStatement] // initialized in prepare
  private def insertCourse(): Future[PreparedStatement] = insertCoursePromise.future
  private val deleteCoursePromise                       = Promise[PreparedStatement] // initialized in prepare
  private def deleteCourse(): Future[PreparedStatement] = deleteCoursePromise.future

  /** Create empty courses table */
  def globalPrepare(): Future[Done] = {
    session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS courses ( " +
        "id TEXT, PRIMARY KEY (id)) ;")
  }

  /** Finishes preparation of CQL Statements */
  def prepare(tag: AggregateEventTag[CourseEvent]): Future[Done] = {
    val prepInsert = session.prepare("INSERT INTO courses (id) VALUES (?) ;")
    insertCoursePromise.completeWith(prepInsert)

    val prepDelete = session.prepare("DELETE FROM courses WHERE id=? ;")
    deleteCoursePromise.completeWith(prepDelete)

    prepInsert.flatMap(_ => prepDelete.map(_ => Done))
  }

  /** EventHandler for OnCourseCreate event */
  def addCourse(eventElement: EventStreamElement[OnCourseCreate]): Future[List[BoundStatement]] = {
    insertCourse().map { ps =>
      val bindWriteTitle = ps.bind()
      bindWriteTitle.setString("id", eventElement.event.course.courseId)
      List(bindWriteTitle)
    }
  }

  /** EventHandler for OnCourseDelete event */
  def deleteCourse(eventElement: EventStreamElement[OnCourseDelete]): Future[List[BoundStatement]] = {
    deleteCourse().map { ps =>
      val bindWriteTitle = ps.bind()
      bindWriteTitle.setString("id", eventElement.event.id)
      List(bindWriteTitle)
    }
  }
}
