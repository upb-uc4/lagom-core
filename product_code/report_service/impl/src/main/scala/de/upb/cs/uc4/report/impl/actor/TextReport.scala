package de.upb.cs.uc4.report.impl.actor

import de.upb.cs.uc4.admission.model.{ CourseAdmission, ExamAdmission }
import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.examresult.model.ExamResultEntry
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{ Format, Json }

case class TextReport(
    user: User,
    certificate: Option[String],
    enrollmentId: String,
    encryptedPrivateKey: Option[EncryptedPrivateKey],
    immatriculationData: Option[ImmatriculationData],
    courses: Option[Seq[Course]],
    courseAdmissions: Option[Seq[CourseAdmission]],
    examAdmissions: Option[Seq[ExamAdmission]],
    exams: Option[Seq[Exam]],
    examResults: Option[Seq[ExamResultEntry]],
    timestamp: String
)

object TextReport {
  implicit val format: Format[TextReport] = Json.format
}