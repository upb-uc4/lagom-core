package de.upb.cs.uc4.shared.server.hyperledger

import akka.Done
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import play.api.libs.json.{Format, Json}

import scala.concurrent.{ExecutionContext, Future}

class HyperLedgerSession(hyperLedgerService: HyperLedgerService)(implicit ec: ExecutionContext) {

  def read[A](transactionId: String)(implicit format: Format[A]): Future[A] =
    read(transactionId, Seq())

  def read[A](transactionId: String, key: String)(implicit format: Format[A]): Future[A] =
    read(transactionId, Seq(key))

  def read[A](transactionId: String, params: Seq[String])(implicit format: Format[A]): Future[A] =
    hyperLedgerService.read(transactionId).invoke(params).map(Json.parse(_).as[A])

  def write[A](transactionId: String, obj: A)(implicit format: Format[A]): Future[Done] =
    write(transactionId, Seq(), Seq(obj))

  def write[A](transactionId: String, seq: Seq[A])(implicit format: Format[A]): Future[Done] =
    write(transactionId, Seq(), seq)

  def write[A](transactionId: String, params: Seq[String], seq: Seq[A])(implicit format: Format[A]): Future[Done] =
    hyperLedgerService.write(transactionId).invoke(params ++ seq.map {
      case s: String => s
      case obj => Json.stringify(Json.toJson(obj))
    })
}
