package de.upb.cs.uc4.course

import de.upb.cs.uc4.course.model.Course
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class CourseSpec extends AsyncWordSpecLike with Matchers {

  val genericString: String = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut l"
  val courseValid: Course = Course("550e8400-e29b-11d4-a716-446655440000", "Name", "Lecture", "2020-04-01", "2020-09-30", 7, "aLecturer", 50, 0, "English", "This is Analysis")

  "A Course" should {
    "be validated" in {
      courseValid.validate.map(_ shouldBe empty)
    }

    //COURSE NAME
    "return a validation error for empty String in courseName" in {
      courseValid.copy(courseName = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("courseName"))
    }

    "return a validation error for incorrect length in courseName" in {
      courseValid.copy(courseName = genericString * 2).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("courseName"))
    }

    //COURSE TYPE
    "return a validation error for incorrect type in courseType" in {
      courseValid.copy(courseType = "Course").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("courseType"))
    }

    //START DATE
    "return a validation error for not existing date in startDate" in {
      courseValid.copy(startDate = "2019-02-29").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("startDate"))
    }
    "return a validation error for incorrect format in startDate" in {
      courseValid.copy(startDate = "2019/1/1").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("startDate"))
    }

    //END DATE
    "return a validation error for not existing date in endDate" in {
      courseValid.copy(endDate = "2019-02-29").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("endDate"))
    }
    "return a validation error for incorrect format in endDate" in {
      courseValid.copy(endDate = "2019/1/1").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("endDate"))
    }

    //ECTS
    "return a validation error for incorrect ects" in {
      courseValid.copy(ects = 0).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("ects"))
    }

    //LECTURER ID
    "return a validation error for empty lecturerId" in {
      courseValid.copy(lecturerId = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("lecturerId"))
    }

    //MAX PARTICIPANTS
    "return a validation error for incorrect maxParticipants" in {
      courseValid.copy(maxParticipants = 0).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("maxParticipants"))
    }

    //CURRENT PARTICIPANTS
    "return a validation error for incorrect currentParticipants" in {
      courseValid.copy(currentParticipants = courseValid.maxParticipants + 1).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("currentParticipants"))
    }

    //COURSE LANGUAGE
    "return a validation error for incorrect courseLanguage" in {
      courseValid.copy(courseLanguage = "Klingon").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("courseLanguage"))
    }

    //COURSE DESCRIPTION
    "return a validation error for incorrect length in courseDescription" in {
      courseValid.copy(courseDescription = genericString * 101).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("courseDescription"))
    }
  }
}

