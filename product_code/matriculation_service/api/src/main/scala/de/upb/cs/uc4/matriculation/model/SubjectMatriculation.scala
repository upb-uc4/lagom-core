package de.upb.cs.uc4.matriculation.model

import de.upb.cs.uc4.shared.client.Utils.SemesterUtils
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

case class SubjectMatriculation(fieldOfStudy: String, semesters: Seq[String]) {
  def trim: SubjectMatriculation =
    copy(fieldOfStudy.trim, semesters.map(_.trim))

  def clean: SubjectMatriculation = {
    trim.copy(semesters = trim.semesters.distinct)
  }

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

    semesters.foreach {
      semester =>
        //Change string from "semester" to "semesters[index]"
        errors :++= semester.validateSemester.map(simpleError => simpleError.copy(name = s"${simpleError.name}s[${semesters.indexOf(semester)}]"))
    }

    errors
  }
}

object SubjectMatriculation {
  implicit val format: Format[SubjectMatriculation] = Json.format
}