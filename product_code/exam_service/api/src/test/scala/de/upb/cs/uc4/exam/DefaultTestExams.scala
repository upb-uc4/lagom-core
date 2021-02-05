package de.upb.cs.uc4.exam

import de.upb.cs.uc4.course.DefaultTestCourses
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.shared.client.configuration.ExamType

trait DefaultTestExams extends DefaultTestCourses {
  //Test exams
  var exam0: Exam = Exam("exam0", course0.courseId, "", course0.lecturerId, ExamType.WrittenExam.toString, "2021-02-02", 8, "2021-01-20", "2021-01-20")
}
