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
    /*
    "return a validation error for having different usernames" in {
      val errors = postMessageStudentValid.copy(authUser = student0Auth.copy(username = "anotherUsername")).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("authUser.username", "student.username")
    }

    "return a validation error for having a non-empty String in latestImmatriculation" in {
      val errors = postMessageStudentValid.copy(student = student0.copy(latestImmatriculation = "SS2020")).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("student.latestImmatriculation")
    }
  }

  "A PostMessageLecturer" should {
    "be validated" in {
      val errors = postMessageLecturerValid.validate
      errors shouldBe empty
    }

    "return a validation error for having different usernames" in {
      val errors = postMessageLecturerValid.copy(authUser = lecturer0Auth.copy(username = "anotherUsername")).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("authUser.username", "lecturer.username")
    }
  }

  "A PostMessageAdmin" should {
    "be validated" in {
      val errors = postMessageAdminValid.validate
      errors shouldBe empty
    }

    "return a validation error for having different usernames" in {
      val errors = postMessageAdminValid.copy(authUser = admin0Auth.copy(username = "anotherUsername")).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("authUser.username", "admin.username")
    }*/
  }
}
