package de.upb.cs.uc4.impl.readside

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement}
import de.upb.cs.uc4.impl.events.{CourseEvent, OnCourseCreate}

import scala.concurrent.{ExecutionContext, Future, Promise}

class CourseDatabase(session: CassandraSession)(implicit ec: ExecutionContext) {

  //Prepared CQL Statements
  private val insertCoursePromise                     = Promise[PreparedStatement] // initialized in prepare
  private def insertCourse(): Future[PreparedStatement] = insertCoursePromise.future

  /** Create empty courses table */
  def globalPrepare(): Future[Done] = {
    session.executeCreateTable(
      "CREATE TABLE IF NOT EXISTS courses ( " +
        "id BIGINT, PRIMARY KEY (id)) ;")
  }

  /** Finishes preparation of CQL Statements */
  def prepare(tag: AggregateEventTag[CourseEvent]): Future[Done] = {
    val prep = session.prepare("INSERT INTO courses (id) VALUES (?) ;")
    insertCoursePromise.completeWith(prep)
    prep.map(_ => Done)
  }

  def addCourse(eventElement: EventStreamElement[OnCourseCreate]): Future[List[BoundStatement]] = {
    insertCourse().map { ps =>
      val bindWriteTitle = ps.bind()
      bindWriteTitle.setLong("id", eventElement.event.course.courseId)
      List(bindWriteTitle)
    }
  }
}
