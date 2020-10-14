package de.upb.cs.uc4.user.model

import de.upb.cs.uc4.shared.client.RegexCollection
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class Address(
    street: String,
    houseNumber: String,
    zipCode: String,
    city: String,
    country: String
) {

  def trim: Address = {
    copy(street.trim, houseNumber.trim, zipCode.trim, city.trim, country.trim)
  }

  /** Validates the parameters of the Address according to syntax, charsets, etc.
    * Returns a Sequence of SimpleErrors[[SimpleError]],
    * that contain the errors that were encountered during validation.
    *
    * @return Sequence of SimpleErrors[[SimpleError]]
    */
  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    val houseNumberRegex = RegexCollection.Address.houseNumberRegex
    val nameRegex = RegexCollection.Address.nameRegex

    val countryList = List("Germany", "United States", "Italy", "France", "United Kingdom", "Belgium", "Netherlands", "Spain", "Austria", "Switzerland", "Poland")

    var errors = List[SimpleError]()
    if (!nameRegex.matches(street)) {
      errors :+= SimpleError("street", "Street must only contain at letters and '-''.")
    }
    houseNumber match {
      case "" =>
        errors :+= SimpleError("houseNumber", "House number must not be empty.")
      case _ if houseNumber.startsWith("0") =>
        errors :+= SimpleError("houseNumber", "House number must not have a leading zero.")
      case _ if !houseNumberRegex.matches(houseNumber) =>
        errors :+= SimpleError("houseNumber", "House number must start with digits and may have trailing letters.")
      case _ =>
    }

    if (!(zipCode forall Character.isDigit) || zipCode.length() != 5) {
      errors :+= SimpleError("zipCode", "Zipcode must consist of exactly five digits.")
    }
    if (!nameRegex.matches(city)) {
      errors :+= SimpleError("city", "City name contains illegal characters.")
    }
    if (!countryList.contains(country)) {
      errors :+= SimpleError("country", "Country must be one of " + countryList.reduce((a, b) => a + ", " + b) + ".")
    }
    errors
  }

}

object Address {
  implicit val format: Format[Address] = Json.format

  val empty: Address = Address("", "", "", "", "")
}
