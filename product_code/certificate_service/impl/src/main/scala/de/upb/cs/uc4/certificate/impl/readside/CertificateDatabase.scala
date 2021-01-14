package de.upb.cs.uc4.certificate.impl.readside

import akka.Done
import akka.util.Timeout
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.{ ExecutionContext, Future }

class CertificateDatabase(database: Database)(implicit ec: ExecutionContext, timeout: Timeout) {

  case class CertificateEntry(username: String, enrollmentId: String)

  /** Table definition of a certificate table */
  class CertificateTable(tag: Tag) extends Table[CertificateEntry](tag, "uc4CertificateTable") {
    def username: Rep[String] = column[String]("username", O.PrimaryKey)
    def enrollmentId: Rep[String] = column[String]("enrollmentId", O.PrimaryKey)

    override def * : ProvenShape[CertificateEntry] =
      (username, enrollmentId) <> ((CertificateEntry.apply _).tupled, CertificateEntry.unapply)
  }

  val table = TableQuery[CertificateTable]

  /** Creates needed table */
  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] =
    table.schema.createIfNotExists

  def get(enrollmentId: String): Future[Option[String]] =
    database.run(find(enrollmentId))

  def set(username: String, enrollmentId: String): Future[Done] =
    database.run(setEnrollmentId(username, enrollmentId))

  private def find(enrollmentId: String): DBIO[Option[String]] =
    table
      .filter(_.enrollmentId === enrollmentId)
      .map(_.username)
      .result
      .headOption

  def setEnrollmentId(username: String, enrollmentId: String): DBIO[Done] =
    table
      .insertOrUpdate(CertificateEntry(username, enrollmentId))
      .map(_ => Done)
      .transactionally

  def deleteEnrollmentId(username: String): DBIO[Done] =
    table
      .filter(_.username === username)
      .delete
      .map(_ => Done)
      .transactionally
}
