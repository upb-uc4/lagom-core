package de.upb.cs.uc4.examreg.model

import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class ExaminationRegulation(name: String, active: Boolean, modules: Seq[Module]) {

  def trim: ExaminationRegulation = copy(name = name.trim, modules = modules.map(_.trim))

  def clean: ExaminationRegulation = trim.copy(modules = modules.map(_.clean))

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate(implicit executionContext: ExecutionContext): Future[Seq[SimpleError]] = {
    val nameRegex = RegexCollection.Commons.nameRegex
    var nameAndActiveErrors = List[SimpleError]()

    if (!nameRegex.matches(name)) {
      nameAndActiveErrors :+= SimpleError("name", ErrorMessageCollection.ExaminationRegulation.nameMessage)
    }
    if (!active) {
      nameAndActiveErrors :+= SimpleError("active", "Active must be set to true.")
    }

    val moduleErrors = modules match {
      case _ if modules.isEmpty => Future.successful(Seq(SimpleError("modules", "An examination regulation must have at least one module.")))
      case _ =>
        Future.sequence {
          modules.map {
            module =>
              module.validate.map {
                _.map {
                  simpleError =>
                    //Name of error is of the form: modules[index].id or modules[index].name
                    simpleError.copy(name = s"module[${modules.indexOf(module)}].${simpleError.name}")
                }
              }
          }
        }.map(_.flatten)
    }

    moduleErrors.map(errors => nameAndActiveErrors ++ errors)
  }
}

object ExaminationRegulation {
  implicit val format: Format[ExaminationRegulation] = Json.format
}