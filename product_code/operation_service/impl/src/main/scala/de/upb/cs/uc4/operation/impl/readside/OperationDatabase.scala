package de.upb.cs.uc4.operation.impl.readside

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.ExecutionContext

class OperationDatabase(database: Database, clusterSharding: ClusterSharding)(implicit ec: ExecutionContext, timeout: Timeout) {

  /** Table definition of a user table */
  abstract class UserTable(tag: Tag, name: String) extends Table[String](tag, s"uc4${name}Table") {
    def username: Rep[String] = column[String]("username", O.PrimaryKey)

    override def * : ProvenShape[String] = username <>
      (username => username, (username: String) => Some(username))
  }

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

  val images = TableQuery[ImageTable]

  /** Creates all needed tables for the different roles */
  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] = {
    images.schema.createIfNotExists
  }
}
