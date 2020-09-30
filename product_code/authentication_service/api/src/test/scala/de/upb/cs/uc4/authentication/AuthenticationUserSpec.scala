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

    //USERNAME
    "return a validation error for incorrect length in username" in {
      authUserValid.copy(username = "Ben").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("username"))
    }
    "return a validation error for invalid character in username" in {
      authUserValid.copy(username = "B€nn").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("username"))
    }

    //PASSWORD
    "return a validation error for empty password" in {
      authUserValid.copy(password = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("password"))
    }
  }
}

