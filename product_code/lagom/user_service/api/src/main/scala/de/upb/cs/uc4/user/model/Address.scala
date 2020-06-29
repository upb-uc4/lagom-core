package de.upb.cs.uc4.user.model

import play.api.libs.json.{Format, Json}

case class Address(street: String,
                   houseNumber: String,
                   zipCode: String,
                   city: String,
                   country: String){

  def trim: Address = {
    copy(street.trim, houseNumber.trim, zipCode.trim, city.trim, country.trim)
  }

  def oneEmpty: Boolean = {street == "" || houseNumber == "" || zipCode == "" || city == "" || country == ""}

}

object Address {
  implicit val format: Format[Address] = Json.format
}
