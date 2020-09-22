package de.upb.cs.uc4.course

import de.upb.cs.uc4.course.model.Course
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CourseSpec extends AnyWordSpecLike with Matchers {

  val genericString: String = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut l"
  val courseValid: Course = Course("550e8400-e29b-11d4-a716-446655440000", "Name", "Lecture", "2020-04-01", "2020-09-30", 7, "aLecturer", 50, 0, "English", "This is Analysis")

  "A Course" should {
    "be validated" in {
      val errors = courseValid.validate
      errors shouldBe empty
    }

    //COURSE NAME
    "return a validation error for empty String in courseName" in {
      val errors = courseValid.copy(courseName = "").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("courseName")
    }
    "return a validation error for incorrect length in courseName" in {
      val errors = courseValid.copy(courseName = genericString * 2).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("courseName")
    }

    //COURSE TYPE
    "return a validation error for incorrect type in courseType" in {
      val errors = courseValid.copy(courseType = "Course").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("courseType")
    }

    //START DATE
    "return a validation error for not existing date in startDate" in {
      val errors = courseValid.copy(startDate = "2019-02-29").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("startDate")
    }
    "return a validation error for incorrect format in startDate" in {
      val errors = courseValid.copy(startDate = "2019/1/1").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("startDate")
    }

    //END DATE
    "return a validation error for not existing date in endDate" in {
      val errors = courseValid.copy(endDate = "2019-02-29").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("endDate")
    }
    "return a validation error for incorrect format in endDate" in {
      val errors = courseValid.copy(endDate = "2019/1/1").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("endDate")
    }

    //ECTS
    "return a validation error for incorrect ects" in {
      val errors = courseValid.copy(ects = 0).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("ects")
    }

    //LECTURER ID
    "return a validation error for empty lecturerId" in {
      val errors = courseValid.copy(lecturerId = "").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("lecturerId")
    }

    //MAX PARTICIPANTS
    "return a validation error for incorrect maxParticipants" in {
      val errors = courseValid.copy(maxParticipants = 0).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("maxParticipants")
    }

    //CURRENT PARTICIPANTS
    "return a validation error for incorrect currentParticipants" in {
      val errors = courseValid.copy(currentParticipants = courseValid.maxParticipants + 1).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("currentParticipants")
    }

    //COURSE LANGUAGE
    "return a validation error for incorrect courseLanguage" in {
      val errors = courseValid.copy(courseLanguage = "Klingon").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("courseLanguage")
    }

    //COURSE DESCRIPTION
    "return a validation error for incorrect length in courseDescription" in {
      val errors = courseValid.copy(courseDescription = genericString * 101).validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("courseDescription")
    }
  }
}

