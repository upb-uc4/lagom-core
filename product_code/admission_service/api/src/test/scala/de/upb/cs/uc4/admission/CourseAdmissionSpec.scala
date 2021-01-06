package de.upb.cs.uc4.admission

import de.upb.cs.uc4.admission.model.CourseAdmission
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class CourseAdmissionSpec extends AsyncWordSpecLike with Matchers {

  val courseAdmissionValid: CourseAdmission = CourseAdmission("", "exampleCourseId", "exampleModuleId", "", "")

  "A CourseAdmission" should {
    "be validated" in {
      courseAdmissionValid.validateOnCreation.map(_ shouldBe empty)
    }

    "return a validation error with enrollmentId non-empty" in {
      courseAdmissionValid.copy(enrollmentId = "exampleEnrollmentId").validateOnCreation.map(_.map(_.name) should contain theSameElementsAs Seq("enrollmentId"))
    }
    "return a validation error with courseId empty" in {
      courseAdmissionValid.copy(courseId = "").validateOnCreation.map(_.map(_.name) should contain theSameElementsAs Seq("courseId"))
    }
    "return a validation error with moduleId empty" in {
      courseAdmissionValid.copy(moduleId = "").validateOnCreation.map(_.map(_.name) should contain theSameElementsAs Seq("moduleId"))
    }
    "return a validation error with admissionId non-empty" in {
      courseAdmissionValid.copy(admissionId = "exampleAdmissionId").validateOnCreation.map(_.map(_.name) should contain theSameElementsAs Seq("admissionId"))
    }
    "return a validation error with timestamp non-empty" in {
      courseAdmissionValid.copy(timestamp = "exampleTimestamp").validateOnCreation.map(_.map(_.name) should contain theSameElementsAs Seq("timestamp"))
    }
  }
}
