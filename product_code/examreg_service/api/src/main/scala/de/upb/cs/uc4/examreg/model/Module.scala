package de.upb.cs.uc4.examreg.model

import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class Module(id: String, name: String) {

  def trim: Module = copy(id.trim, name.trim)

  def clean: Module = trim

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate(implicit executionContext: ExecutionContext): Future[Seq[SimpleError]] = Future {
    var errors = List[SimpleError]()

    val idRegex = RegexCollection.Module.idRegex
    val nameRegex = RegexCollection.Commons.nameRegex

    if (!idRegex.matches(id)) {
      errors :+= SimpleError("id", ErrorMessageCollection.Module.idMessage)
    }
    if (!nameRegex.matches(name)) {
      errors :+= SimpleError("name", ErrorMessageCollection.Module.nameMessage)
    }
    errors
  }
}

object Module {
  implicit val format: Format[Module] = Json.format
}