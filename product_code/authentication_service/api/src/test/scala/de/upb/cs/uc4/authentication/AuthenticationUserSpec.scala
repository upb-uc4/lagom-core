package de.upb.cs.uc4.authentication

import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class AuthenticationUserSpec extends AsyncWordSpecLike with Matchers {

  val genericString: String = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut l"
  val authUserValid: AuthenticationUser = AuthenticationUser("username2", "password123", AuthenticationRole.Admin)

  "An AuthenticationUser" should {
    "be validated" in {
      authUserValid.validate.map(_ shouldBe empty)
    }
    /*
    //USERNAME
    "return a validation error for incorrect length in username" in {
      val errors = authUserValid.copy(username = "Ben").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("username")
    }
    "return a validation error for invalid character in username" in {
      val errors = authUserValid.copy(username = "B€nn").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("username")
    }

    //PASSWORD
    "return a validation error for empty password" in {
      val errors = authUserValid.copy(password = "").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("password")
    }*/
  }
}

