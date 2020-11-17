package de.upb.cs.uc4.user

import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student }
import de.upb.cs.uc4.user.model.{ Address, Role }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class UserSpec extends AsyncWordSpecLike with Matchers {

  val genericString: String = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut l"
  val addressValid: Address = Address("Gänseweg", "42a", "13337", "Entenhausen", "Germany")

  val studentValid: Student = Student("student0", "c3R1ZGVudHN0dWRlbnQ=", Role.Student, addressValid, "VollDer", "Hammer", "example@mail.de", "+49123456789", "1990-12-11", "SS2020", "7421769")
  val lecturerValid: Lecturer = Lecturer("lecturer0", "bGVjdHVyZXJsZWN0dXJlcg==", Role.Lecturer, addressValid, "EchtDer", "Hammer", "example@mail.de", "+49123456789", "1991-12-11", "Heute kommt der kleine Gauss dran.", "Mathematics")
  val adminValid: Admin = Admin("admin0", "YWRtaW5hZG1pbg==", Role.Admin, addressValid, "firstName", "LastName", "example@mail.de", "+49123456789", "1992-12-11")

  "A User" should {

    "discard private fields for admins" in {
      adminValid.toPublic should ===(adminValid.copy(address = Address.empty, birthDate = ""))
    }

    "discard private fields for lecturers" in {
      lecturerValid.toPublic should ===(lecturerValid.copy(address = Address.empty, birthDate = ""))
    }

    "discard private fields for students" in {
      studentValid.toPublic should ===(studentValid.copy(address = Address.empty, birthDate = "", latestImmatriculation = "", matriculationId = ""))
    }

    "be validated" in {
      adminValid.validate.map(_ shouldBe empty)
    }

    //USERNAME
    "return a validation error for incorrect length in username" in {
      adminValid.copy(username = "Ben").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("username"))
    }
    "return a validation error for invalid character in username" in {
      adminValid.copy(username = "B€nn").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("username"))
    }

    //ROLE
    "return a validation error for incorrect role in role" in {
      adminValid.copy(role = Role.Student).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("role"))
    }

    //FIRST NAME
    "return a validation error for incorrect length in firstName" in {
      adminValid.copy(firstName = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("firstName"))
    }

    //LAST NAME
    "return a validation error for incorrect length in lastName" in {
      adminValid.copy(lastName = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("lastName"))
    }

    //EMAIL
    "return a validation error for incorrect format in email" in {
      adminValid.copy(email = "examplemail.com").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("email"))
    }

    //PHONE NUMBER
    "return a validation error for non-digits in phoneNumber" in {
      adminValid.copy(phoneNumber = "+five").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("phoneNumber"))
    }
    "return a validation error for missing '+' in phoneNumber" in {
      adminValid.copy(phoneNumber = "42").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("phoneNumber"))
    }
    "return a validation error for incorrect length in phoneNumber" in {
      adminValid.copy(phoneNumber = "+31415926535897932384626433832795028842973").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("phoneNumber"))
    }

    //BIRTH DATE
    "return a validation error for not existing date in birthDate" in {
      adminValid.copy(birthDate = "2019-02-29").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("birthDate"))
    }
    "return a validation error for incorrect format in birthDate" in {
      adminValid.copy(birthDate = "2019/1/1").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("birthDate"))
    }
  }

  "A Lecturer" should {
    "be validated" in {
      lecturerValid.validate.map(_ shouldBe empty)
    }

    //FREE TEXT
    "return a validation error for incorrect length in freeText" in {
      lecturerValid.copy(freeText = genericString * 101).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("freeText"))
    }

    //RESEARCH AREA
    "return a validation error for incorrect length in researchArea" in {
      lecturerValid.copy(researchArea = genericString * 3).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("researchArea"))
    }
  }

  "A Student" should {
    "be validated" in {
      studentValid.validate.map(_ shouldBe empty)
    }

    //LATEST IMMATRICULATION
    "be validated with empty latestImmatriculation" in {
      studentValid.copy(latestImmatriculation = "").validate.map(_ shouldBe empty)
    }
    "return a validation error for incorrect semester in latestImmatriculation" in {
      studentValid.copy(latestImmatriculation = "FS2020").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("latestImmatriculation"))
    }

    //MATRICULATION ID
    "return a validation error for incorrect format in matriculationId" in {
      studentValid.copy(matriculationId = "0000000").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("matriculationId"))
    }
    "return a validation error for incorrect length in matriculationId" in {
      studentValid.copy(matriculationId = "00042").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("matriculationId"))
    }
  }

  "An Address" should {
    "be validated" in {
      addressValid.validate.map(_ shouldBe empty)
    }
    //STREET
    "return a validation error for incorrect length in street" in {
      addressValid.copy(street = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("street"))
    }
    "return a validation error for incorrect character in street" in {
      addressValid.copy(street = "Entenstra?e").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("street"))
    }

    //HOUSE NUMBER
    "return a validation error for empty String in houseNumber" in {
      addressValid.copy(houseNumber = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("houseNumber"))
    }
    "return a validation error for leading zero in houseNumber" in {
      addressValid.copy(houseNumber = "02").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("houseNumber"))
    }
    "return a validation error for wrong format in houseNumber" in {
      addressValid.copy(houseNumber = "a3").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("houseNumber"))
    }

    //ZIPCODE
    "return a validation error for wrong length in zipCode" in {
      addressValid.copy(zipCode = "42").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("zipCode"))
    }
    "return a validation error for non-digits in zipCode" in {
      addressValid.copy(zipCode = "eight").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("zipCode"))
    }

    //CITY
    "return a validation error for incorrect length in city" in {
      addressValid.copy(city = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("city"))
    }
    "return a validation error for incorrect character in city" in {
      addressValid.copy(city = "&enhausen").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("city"))
    }

    //COUNTRY
    "return a validation error for undefined country" in {
      addressValid.copy(country = "Wakanda").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("country"))
    }
  }
}
