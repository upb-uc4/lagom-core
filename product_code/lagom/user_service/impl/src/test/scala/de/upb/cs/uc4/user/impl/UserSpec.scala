package de.upb.cs.uc4.user.impl

import de.upb.cs.uc4.user.model.user.{Admin, Lecturer, Student}
import de.upb.cs.uc4.user.model.{Address, Role}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UserSpec extends AnyWordSpecLike with Matchers {
  
  val address0: Address = Address("Gaenseweg", "42a", "13337", "Entenhausen", "Germany")
  val address1: Address = Address("643n53w3g?", "b42a", "1337", "R164-?", "1377l4nd")

  val student0: Student = Student("student0", Role.Student, address0, "VollDer", "Hammer", "Picture", "example@mail.de", "+49123456789", "1990-12-11", "IN", "7421769", 9000, List())
  val student1: Student = Student("student0", Role.Lecturer, address0, "", "", "Picture", "org.exampleatmail", "49 123456789","11-12-1996a", "--a,s@", "matriculationText", -7, List("(╯°□°）╯︵ ┻━┻"))
  val lecturer0: Lecturer = Lecturer("lecturer0", Role.Lecturer, address0, "EchtDer", "Hammer", "Picture", "example@mail.de", "+49123456789", "1991-12-11", "Heute kommt der kleine Gauss dran.", "Mathematics")
  val lecturer1: Lecturer = Lecturer("lecturer0", Role.Student, address0, "", "", "P1ctur3", "130SS@@mail.de","49 123456789", "19912-121-111", "╬þv¦e1┐‗☺❤🍕🍕😘😒", "🏆Pokemon Trainer💦")
  val admin0: Admin = Admin("admin0", Role.Admin, address0, "firstName", "LastName", "Picture", "example@mail.de","+49123456789", "1992-12-11")
  val admin1: Admin = Admin("admin0", Role.Student, address0, "", "", "🔫🔫🔫🔫", "example.mail@de","49 123456789", "Gestern, den 12.11.1995")



  

  "An Admin" should {
    "be validated" in {
      val errors = admin0.validate
      errors shouldBe empty
    }
    "return validation errors" in {
      val errors = admin1.validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain only ("role","email", "phoneNumber", "birthDate", "firstName", "lastName")
    }
  }
  
  "A Lecturer" should {
    "be validated" in {
      val errors = lecturer0.validate
      errors shouldBe empty
    }
    "return validation errors" in {
      val errors = lecturer1.validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain only ("role","email","phoneNumber","birthDate", "firstName", "lastName")
    }
  }

  "A Student" should {
      "be validated" in {
        val errors = student0.validate
        errors shouldBe empty
      }
      "return validation errors" in {
        val errors = student1.validate
        val errorVariables = errors.map(error => error.name)
        errorVariables should contain only ("role","email","phoneNumber","birthDate", "firstName", "lastName","semesterCount","matriculationId","fieldsOfStudy")
      }
  }

  "An Adress" should {
      "be validated" in {
      val errors = address0.validate
      errors shouldBe empty
      }
      "return validation errors" in {
        val errors = address1.validate
        val errorVariables = errors.map(error => error.name)
        errorVariables should contain only ("street","houseNumber","zipCode","city","country")
      }
  }

}
