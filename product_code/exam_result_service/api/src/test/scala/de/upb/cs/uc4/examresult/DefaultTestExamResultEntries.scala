package de.upb.cs.uc4.examresult

import de.upb.cs.uc4.exam.DefaultTestExams
import de.upb.cs.uc4.examresult.model.ExamResultEntry
import de.upb.cs.uc4.shared.client.configuration.ConfigurationCollection

trait DefaultTestExamResultEntries extends DefaultTestExams {

  //Test exam entries
  var examResultEntry0: ExamResultEntry = ExamResultEntry("", exam0.examId, ConfigurationCollection.grades.head)
}
