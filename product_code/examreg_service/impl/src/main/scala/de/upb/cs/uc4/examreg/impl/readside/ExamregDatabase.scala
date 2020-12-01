package de.upb.cs.uc4.examreg.impl.readside

import akka.Done
import de.upb.cs.uc4.examreg.model.ExaminationRegulation
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.{ ExecutionContext, Future }

class ExamregDatabase(database: Database)(implicit ec: ExecutionContext) {

  /** Table definition of a examination regulations table */
  class ExamregTable(tag: Tag) extends Table[String](tag, "uc4ExamregTable") {
    def name: Rep[String] = column[String]("name", O.PrimaryKey)

    override def * : ProvenShape[String] = name <>
      (name => name, (name: String) => Some(name))
  }

  val examregs = TableQuery[ExamregTable]

  /** Creates needed table */
  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] =
    examregs.schema.createIfNotExists

  /** Returns a Sequence of all examination regulations */
  def getAll: Future[Seq[String]] =
    database.run(findAllQuery)

  /** Adds a examination regulation to the table
    *
    * @param examreg which should get added
    */
  def addExamreg(examreg: ExaminationRegulation): DBIO[Done] = {
    findByExamregNameQuery(examreg.name)
      .flatMap {
        case None => examregs += examreg.name
        case _    => DBIO.successful(Done)
      }
      .map(_ => Done)
      .transactionally
  }

  /** Returns the query to get all examination regulations */
  private def findAllQuery: DBIO[Seq[String]] = examregs.result

  /** Returns the query to find a examination regulation by its name */
  private def findByExamregNameQuery(name: String): DBIO[Option[String]] =
    examregs
      .filter(_.name === name)
      .result
      .headOption
}
