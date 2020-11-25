package de.upb.cs.uc4.user

import de.upb.cs.uc4.user.model.PostMessageUser
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class PostMessageUserSpec extends AsyncWordSpecLike with Matchers with DefaultTestUsers {

  private val postMessageUserValid = PostMessageUser(student0Auth, student0)

  "A PostMessageUser" should {
    "be validated" in {
      postMessageUserValid.validate.map(_ shouldBe empty)
    }

    "return a validation error for having no governmentId" in {
      postMessageUserValid.copy(governmentId = "", user = student0.copy(enrollmentIdSecret = "")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("governmentId"))
    }

    "return a validation error for having different usernames" in {
      postMessageUserValid.copy(authUser = student0Auth.copy(username = "anotherUsername")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("authUser.username", "user.username"))
    }

    "return a validation error for having a non-empty String in latestImmatriculation" in {
      postMessageUserValid.copy(user = student0.copy(latestImmatriculation = "SS2020")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("user.latestImmatriculation"))
    }

    "return a validation error for having an enrollmentIdSecret in user" in {
      postMessageUserValid.copy(user = student0.copy(enrollmentIdSecret = "something")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("user.enrollmentIdSecret"))
    }
  }
}
