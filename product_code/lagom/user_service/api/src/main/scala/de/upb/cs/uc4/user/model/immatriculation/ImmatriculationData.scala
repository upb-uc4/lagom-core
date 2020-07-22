package de.upb.cs.uc4.user.model.immatriculation

import de.upb.cs.uc4.user.model.user.Student
import play.api.libs.json.{Format, Json}

case class ImmatriculationData(matriculationId: String,
                               firstName: String,
                               lastName: String,
                               birthDate: String,
                               immatriculationStatus: Seq[ImmatriculationStatus])

object ImmatriculationData {

  def apply(student: Student, immatriculationStatus: ImmatriculationStatus*) =
    new ImmatriculationData(student.matriculationId, student.firstName, student.lastName, student.birthDate,
      immatriculationStatus)

  implicit val format: Format[ImmatriculationData] = Json.format
}