package de.upb.cs.uc4.admission.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class DropAdmission(admissionId: String) {

  def trim: DropAdmission = copy(admissionId.trim)

  def validateOnCreation(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    var errors = List[SimpleError]()

    if (admissionId.nonEmpty) {
      errors :+= SimpleError("admissionId", "AdmissionId must be empty.")
    }

    errors
  }
}

object DropAdmission {
  implicit val format: Format[DropAdmission] = Json.format
}
