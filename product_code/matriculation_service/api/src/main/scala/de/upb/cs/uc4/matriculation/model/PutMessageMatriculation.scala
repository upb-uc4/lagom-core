package de.upb.cs.uc4.matriculation.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

case class PutMessageMatriculation(matriculation: Seq[SubjectMatriculation]) {

  def trim: PutMessageMatriculation = copy(matriculation.map(_.trim))

  def clean: PutMessageMatriculation = copy(matriculation.map(_.clean))

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate: Seq[SimpleError] = {

    var errors = List[SimpleError]()

    matriculation.foreach {
      subjectMatriculation =>
        errors :++= subjectMatriculation.validate.map {
          simpleError =>
            //Name of error is of the form: matriculation[index].semesters[index]
            simpleError.copy(name = s"matriculation[${matriculation.indexOf(subjectMatriculation)}].${simpleError.name}")
        }
    }

    errors
  }
}

object PutMessageMatriculation {
  implicit val format: Format[PutMessageMatriculation] = Json.format
}
