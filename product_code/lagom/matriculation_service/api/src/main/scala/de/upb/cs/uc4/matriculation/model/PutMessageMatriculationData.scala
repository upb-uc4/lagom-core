package de.upb.cs.uc4.matriculation.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{Format, Json}

case class PutMessageMatriculationData(fieldOfStudy: String, semester: String) {

  def trim: PutMessageMatriculationData = copy(fieldOfStudy.trim, semester.trim)

  /**
   * Validates the object by checking predefined conditions like correct charsets, syntax, etc.
   * Returns a list of SimpleErrors[[SimpleError]]
   *
   * @return Filled Sequence of [[SimpleError]]
   */
  def validate: Seq[SimpleError] = {

    val fos = List("Computer Science","Philosophy","Media Sciences", "Economics", "Mathematics", "Physics", "Chemistry",
      "Education", "Sports Science", "Japanology", "Spanish Culture", "Pedagogy", "Business Informatics", "Linguistics")

    // Regex for semesters, accepts for example "SS2020" and "WS2020/21"
    val semesterRegex = """(WS[1-9][0-9]{3}/[0-9]{2})|(SS[1-9][0-9]{3})""".r

    var errors = List[SimpleError]()

    if (!fos.contains(fieldOfStudy)) {
      errors :+= SimpleError("fieldOfStudy",
        "Field of study must be one of " + fos.reduce((a,b) => a+", "+b) + ".")
    }
    if(!semesterRegex.matches(semester)){
      errors :+= SimpleError("semester", "Semester must be of the format \"SSyyyy\" for summer, \"WSyyyy/yy\" for winter.")
    }else{
      if (semester.substring(0,2) == "WS" && (semester.substring(4, 6).toInt+1 != semester.substring(7,9).toInt)){
        errors :+= SimpleError("semester", "Winter semester must consist of two consecutive years.")
      }
    }
    errors
  }
}

object PutMessageMatriculationData {
  implicit val format: Format[PutMessageMatriculationData] = Json.format
}
