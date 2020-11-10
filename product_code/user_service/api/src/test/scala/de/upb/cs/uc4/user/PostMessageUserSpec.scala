package de.upb.cs.uc4.user

import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class PostMessageUserSpec extends AsyncWordSpecLike with Matchers with DefaultTestUsers {

  private val postMessageStudentValid = PostMessageStudent(student0Auth, student0)
  private val postMessageLecturerValid = PostMessageLecturer(lecturer0Auth, lecturer0)
  private val postMessageAdminValid = PostMessageAdmin(admin0Auth, admin0)

  "A PostMessageStudent" should {
    "be validated" in {
      postMessageStudentValid.validate.map(_ shouldBe empty)
    }

    "return a validation error for having different usernames" in {
      postMessageStudentValid.copy(authUser = student0Auth.copy(username = "anotherUsername")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("authUser.username", "student.username"))
    }

    "return a validation error for having a non-empty String in latestImmatriculation" in {
      postMessageStudentValid.copy(user = student0.copy(latestImmatriculation = "SS2020")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("student.latestImmatriculation"))
    }
  }

  "A PostMessageLecturer" should {
    "be validated" in {
      postMessageLecturerValid.validate.map(_ shouldBe empty)
    }

    "return a validation error for having different usernames" in {
      postMessageLecturerValid.copy(authUser = lecturer0Auth.copy(username = "anotherUsername")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("authUser.username", "lecturer.username"))
    }
  }

  "A PostMessageAdmin" should {
    "be validated" in {
      postMessageAdminValid.validate.map(_ shouldBe empty)
    }

    "return a validation error for having different usernames" in {
      postMessageAdminValid.copy(authUser = admin0Auth.copy(username = "anotherUsername")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("authUser.username", "admin.username"))
    }
  }
}
