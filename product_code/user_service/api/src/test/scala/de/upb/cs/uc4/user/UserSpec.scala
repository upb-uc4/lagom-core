package de.upb.cs.uc4.user

import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student }
import de.upb.cs.uc4.user.model.{ Address, Role }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserSpec extends AnyWordSpecLike with Matchers {

  val genericString: String = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut l"
  val addressValid: Address = Address("Gänseweg", "42a", "13337", "Entenhausen", "Germany")

  val studentValid: Student = Student("student0", Role.Student, addressValid, "VollDer", "Hammer", "Picture", "example@mail.de", "+49123456789", "1990-12-11", "SS2020", "7421769")
  val lecturerValid: Lecturer = Lecturer("lecturer0", Role.Lecturer, addressValid, "EchtDer", "Hammer", "Picture", "example@mail.de", "+49123456789", "1991-12-11", "Heute kommt der kleine Gauss dran.", "Mathematics")
  val adminValid: Admin = Admin("admin0", Role.Admin, addressValid, "firstName", "LastName", "Picture", "example@mail.de", "+49123456789", "1992-12-11")

  "A User" should {
    "be validated" in {
      val errors = adminValid.validate
      errors shouldBe empty
    }

    //USERNAME
    "return a validation error for incorrect length in username" in {
      val errors = adminValid.copy(username = "Ben").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("username")
    }
    "return a validation error for invalid character in username" in {
      val errors = adminValid.copy(username = "B€nn").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("username")
    }

    //ROLE
    "return a validation error for incorrect role in role" in {
      val errors = adminValid.copy(role = Role.Student).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("role")
    }

    //FIRST NAME
    "return a validation error for incorrect length in firstName" in {
      val errors = adminValid.copy(firstName = "").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("firstName")
    }

    //LAST NAME
    "return a validation error for incorrect length in lastName" in {
      val errors = adminValid.copy(lastName = "").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("lastName")
    }

    //PICTURE
    "return a validation error for incorrect length in picture" in {
      val errors = adminValid.copy(picture = genericString * 3).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("picture")
    }

    //EMAIL
    "return a validation error for incorrect format in email" in {
      val errors = adminValid.copy(email = "examplemail.com").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("email")
    }

    //PHONE NUMBER
    "return a validation error for non-digits in phoneNumber" in {
      val errors = adminValid.copy(phoneNumber = "+five").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("phoneNumber")
    }
    "return a validation error for missing '+' in phoneNumber" in {
      val errors = adminValid.copy(phoneNumber = "42").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("phoneNumber")
    }
    "return a validation error for incorrect length in phoneNumber" in {
      val errors = adminValid.copy(phoneNumber = "+31415926535897932384626433832795028842973").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("phoneNumber")
    }

    //BIRTH DATE
    "return a validation error for not existing date in birthDate" in {
      val errors = adminValid.copy(birthDate = "2019-02-29").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("birthDate")
    }
    "return a validation error for incorrect format in birthDate" in {
      val errors = adminValid.copy(birthDate = "2019/1/1").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("birthDate")
    }
  }

  "A Lecturer" should {
    "be validated" in {
      val errors = lecturerValid.validate
      errors shouldBe empty
    }

    //FREE TEXT
    "return a validation error for incorrect length in freeText" in {
      val errors = lecturerValid.copy(freeText = genericString * 101).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("freeText")
    }

    //RESEARCH AREA
    "return a validation error for incorrect length in researchArea" in {
      val errors = lecturerValid.copy(researchArea = genericString * 3).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("researchArea")
    }
  }

  "A Student" should {
    "be validated" in {
      val errors = studentValid.validate
      errors shouldBe empty
    }

    //LATEST IMMATRICULATION
    "be validated with empty latestImmatriculation" in {
      val errors = studentValid.copy(latestImmatriculation = "").validate
      errors shouldBe empty
    }
    "return a validation error for incorrect semester in latestImmatriculation" in {
      val errors = studentValid.copy(latestImmatriculation = "FS2020").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("latestImmatriculation")
    }

    //MATRICULATION ID
    "return a validation error for incorrect format in matriculationId" in {
      val errors = studentValid.copy(matriculationId = "0000000").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("matriculationId")
    }
    "return a validation error for incorrect length in matriculationId" in {
      val errors = studentValid.copy(matriculationId = "00042").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("matriculationId")
    }
  }

  "An Address" should {
    "be validated" in {
      val errors = addressValid.validate
      errors shouldBe empty
    }
    //STREET
    "return a validation error for incorrect length in street" in {
      val errors = addressValid.copy(street = "").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("street")
    }
    "return a validation error for incorrect character in street" in {
      val errors = addressValid.copy(street = "Entenstra?e").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("street")
    }

    //HOUSE NUMBER
    "return a validation error for empty String in houseNumber" in {
      val errors = addressValid.copy(houseNumber = "").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("houseNumber")
    }
    "return a validation error for leading zero in houseNumber" in {
      val errors = addressValid.copy(houseNumber = "02").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("houseNumber")
    }
    "return a validation error for wrong format in houseNumber" in {
      val errors = addressValid.copy(houseNumber = "a3").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("houseNumber")
    }

    //ZIPCODE
    "return a validation error for wrong length in zipCode" in {
      val errors = addressValid.copy(zipCode = "42").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("zipCode")
    }
    "return a validation error for non-digits in zipCode" in {
      val errors = addressValid.copy(zipCode = "eight").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("zipCode")
    }

    //CITY
    "return a validation error for incorrect length in city" in {
      val errors = addressValid.copy(city = "").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("city")
    }
    "return a validation error for incorrect character in city" in {
      val errors = addressValid.copy(city = "&enhausen").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("city")
    }

    //COUNTRY
    "return a validation error for undefined country" in {
      val errors = addressValid.copy(country = "Wakanda").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("country")
    }
  }
}
