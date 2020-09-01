package de.upb.cs.uc4.course.impl.readside

import akka.Done
import de.upb.cs.uc4.course.model.Course
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.{ ExecutionContext, Future }

class CourseDatabase(database: Database)(implicit ec: ExecutionContext) {

  /** Table definition of a course table */
  class CourseTable(tag: Tag) extends Table[String](tag, "uc4CourseTable") {
    def id: Rep[String] = column[String]("id", O.PrimaryKey)

    override def * : ProvenShape[String] = id <>
      (id => id, (id: String) => Some(id))
  }

  val courses = TableQuery[CourseTable]

  /** Creates needed table */
  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] =
    courses.schema.createIfNotExists

  /** Returns a Sequence of all courses */
  def getAll: Future[Seq[String]] =
    database.run(findAllQuery)

  /** Adds a course to the table
    *
    * @param course which should get added
    */
  def addCourse(course: Course): DBIO[Done] = {
    findByCourseIdQuery(course.courseId)
      .flatMap {
        case None => courses += course.courseId
        case _    => DBIO.successful(Done)
      }
      .map(_ => Done)
      .transactionally
  }

  /** Deletes a course from the table
    *
    * @param id of the course which should get added
    */
  def removeCourse(id: String): DBIO[Done] = {
    courses
      .filter(_.id === id)
      .delete
      .map(_ => Done)
      .transactionally
  }

  /** Returns the query to get all courses */
  private def findAllQuery: DBIO[Seq[String]] = courses.result

  /** Returns the query to find a course by its id */
  private def findByCourseIdQuery(courseId: String): DBIO[Option[String]] =
    courses
      .filter(_.id === courseId)
      .result
      .headOption
}
