package de.upb.cs.uc4.matriculation.model

import de.upb.cs.uc4.shared.client.Utils.SemesterUtils
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

case class PutMessageMatriculationData(fieldOfStudy: String, semester: String) {

  def trim: PutMessageMatriculationData = copy(fieldOfStudy.trim, semester.trim)

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate: Seq[SimpleError] = {

    val fos = List("Computer Science", "Philosophy", "Media Sciences", "Economics", "Mathematics", "Physics", "Chemistry",
      "Education", "Sports Science", "Japanology", "Spanish Culture", "Pedagogy", "Business Informatics", "Linguistics")

    var errors = List[SimpleError]()

    if (!fos.contains(fieldOfStudy)) {
      errors :+= SimpleError(
        "fieldOfStudy",
        "Field of study must be one of " + fos.reduce((a, b) => a + ", " + b) + "."
      )
    }
    errors :++= semester.validateSemester

    errors
  }
}

object PutMessageMatriculationData {
  implicit val format: Format[PutMessageMatriculationData] = Json.format
}
