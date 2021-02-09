package de.upb.cs.uc4.examresult

import de.upb.cs.uc4.exam.DefaultTestExams
import de.upb.cs.uc4.examresult.model.ExamResultEntry
import de.upb.cs.uc4.shared.client.configuration.ConfigurationCollection
import de.upb.cs.uc4.user.DefaultTestUsers

trait DefaultTestExamResultEntries extends DefaultTestExams with DefaultTestUsers {

  //Test exam entries
  var exam0ResultEntry0: ExamResultEntry = ExamResultEntry(student0.username + "enrollmentId", exam0.examId, ConfigurationCollection.grades.head)
  var exam0ResultEntry1: ExamResultEntry = ExamResultEntry(student1.username + "enrollmentId", exam0.examId, ConfigurationCollection.grades.apply(1))
  var exam0ResultEntry2: ExamResultEntry = ExamResultEntry(student2.username + "enrollmentId", exam0.examId, ConfigurationCollection.grades.apply(2))

  var exam1ResultEntry0: ExamResultEntry = ExamResultEntry(student0.username + "enrollmentId", exam1.examId, ConfigurationCollection.grades.head)
  var exam1ResultEntry1: ExamResultEntry = ExamResultEntry(student1.username + "enrollmentId", exam1.examId, ConfigurationCollection.grades.apply(1))
  var exam1ResultEntry2: ExamResultEntry = ExamResultEntry(student2.username + "enrollmentId", exam1.examId, ConfigurationCollection.grades.apply(2))

  var exam3ResultEntry0: ExamResultEntry = ExamResultEntry(student0.username + "enrollmentId", exam3.examId, ConfigurationCollection.grades.head)
  var exam3ResultEntry1: ExamResultEntry = ExamResultEntry(student1.username + "enrollmentId", exam3.examId, ConfigurationCollection.grades.apply(1))
  var exam3ResultEntry2: ExamResultEntry = ExamResultEntry(student2.username + "enrollmentId", exam3.examId, ConfigurationCollection.grades.apply(2))

}
