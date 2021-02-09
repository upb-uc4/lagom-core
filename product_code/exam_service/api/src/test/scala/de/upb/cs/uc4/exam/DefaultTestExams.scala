package de.upb.cs.uc4.exam

import de.upb.cs.uc4.course.DefaultTestCourses
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.shared.client.configuration.ExamType

trait DefaultTestExams extends DefaultTestCourses {
  //Test exams
  var exam0: Exam = Exam("exam0", course0.courseId, "", course0.lecturerId + "enrollmentId", ExamType.WrittenExam.toString, "2021-02-02", 8, "2021-01-20", "2021-01-20")
  var exam1: Exam = Exam("exam1", course0.courseId, "", course0.lecturerId + "enrollmentId", ExamType.WrittenExam.toString, "2021-02-28", 8, "2021-02-16", "2021-02-16")
  var exam2: Exam = Exam("exam2", course1.courseId, "", course1.lecturerId + "enrollmentId", ExamType.WrittenExam.toString, "2021-02-02", 8, "2021-01-20", "2021-01-20")
  var exam3: Exam = Exam("exam3", course2.courseId, "", course2.lecturerId + "enrollmentId", ExamType.WrittenExam.toString, "2021-02-28", 8, "2021-02-16", "2021-02-16")
}
