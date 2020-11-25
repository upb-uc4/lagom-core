package de.upb.cs.uc4.user

import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class PostMessageUserSpec extends AsyncWordSpecLike with Matchers with DefaultTestUsers {

  private val postMessageStudentValid = PostMessageStudent(student0Auth, "governmentIdStudent", student0)
  private val postMessageLecturerValid = PostMessageLecturer(lecturer0Auth, "governmentIdLecturer", lecturer0)
  private val postMessageAdminValid = PostMessageAdmin(admin0Auth, "governmentIdAdmin", admin0)

  "A PostMessageUser" should {

    "return a validation error for having no governmentId" in {
      postMessageStudentValid.copy(governmentId = "", student = student0.copy(enrollmentIdSecret = "")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("governmentId"))
    }
  }

  "A PostMessageStudent" should {
    "be validated" in {
      postMessageStudentValid.validate.map(_ shouldBe empty)
    }

    "return a validation error for having different usernames" in {
      postMessageStudentValid.copy(authUser = student0Auth.copy(username = "anotherUsername")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("authUser.username", "student.username"))
    }

    "return a validation error for having a non-empty String in latestImmatriculation" in {
      postMessageStudentValid.copy(student = student0.copy(latestImmatriculation = "SS2020")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("student.latestImmatriculation"))
    }

    "return a validation error for having an enrollmentIdSecret in student" in {
      postMessageStudentValid.copy(student = student0.copy(enrollmentIdSecret = "something")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("student.enrollmentIdSecret"))
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

    "return a validation error for having an enrollmentIdSecret in lecturer" in {
      postMessageLecturerValid.copy(lecturer = lecturer0.copy(enrollmentIdSecret = "something")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("lecturer.enrollmentIdSecret"))
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

    "return a validation error for having an enrollmentIdSecret in admin" in {
      postMessageAdminValid.copy(admin = admin0.copy(enrollmentIdSecret = "something")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("admin.enrollmentIdSecret"))
    }
  }
}
