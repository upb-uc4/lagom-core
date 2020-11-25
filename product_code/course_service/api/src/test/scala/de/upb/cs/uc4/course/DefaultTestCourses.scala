package de.upb.cs.uc4.course

import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.client.configuration.{ CourseLanguage, CourseType }

trait DefaultTestCourses {
  //Test courses
  var course0: Course = Course("", Seq(), "Course 0", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "lecturer0", 60, 20, CourseLanguage.German.toString, "A test")
  var course1: Course = Course("", Seq(), "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "lecturer0", 60, 20, CourseLanguage.German.toString, "A test")
  var course2: Course = Course("", Seq(), "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "lecturer1", 60, 20, CourseLanguage.German.toString, "A test")
  var course3: Course = Course("", Seq(), "Course 3", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "lecturer0", 60, 20, CourseLanguage.German.toString, "A test")

}
