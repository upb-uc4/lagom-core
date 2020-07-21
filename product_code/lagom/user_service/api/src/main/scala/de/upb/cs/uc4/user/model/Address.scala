package de.upb.cs.uc4.user.model

import de.upb.cs.uc4.shared.client.SimpleError
import play.api.libs.json.{Format, Json}

case class Address(street: String,
                   houseNumber: String,
                   zipCode: String,
                   city: String,
                   country: String){

  def trim: Address = {
    copy(street.trim, houseNumber.trim, zipCode.trim, city.trim, country.trim)
  }

  def validate: Seq[SimpleError] = {
    val houseNumberRegex = """[0-9]+[a-z]""".r
    val nameRegex = """[a-zA-Z.-]+""".r
   
    var errors = List[SimpleError]()
    if (!nameRegex.matches(street)){
      errors :+= SimpleError("street", "Street must contain at least one letter.")
    }
    if (!houseNumberRegex.matches(houseNumber)){
      errors :+= SimpleError("houseNumber","House number must contain only digit and a trailing letter.")
    }
    if (!(zipCode forall Character.isDigit) || zipCode.length() != 5){
      errors :+= SimpleError("zipCode","Zipcode must contain exactly 5 digits.")
    }
    if (!nameRegex.matches(city)){
      errors :+= SimpleError("city", "City name must be valid.")
    }
    if (!nameRegex.matches(country)){
      errors :+= SimpleError("country", "Country name must be valid.")
    }
    errors
  }

}

object Address {
  implicit val format: Format[Address] = Json.format
}
