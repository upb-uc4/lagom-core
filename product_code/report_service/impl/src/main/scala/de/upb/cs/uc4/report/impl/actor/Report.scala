package de.upb.cs.uc4.report.impl.actor

import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{ Format, Json }

case class Report(user: User,
                  certificate: String,
                  enrollmentId: String,
                  encryptedPrivateKey: EncryptedPrivateKey,
                  immatriculationData: Option[ImmatriculationData],
                  courses: Option[Seq[Course]],
                  timestamp: String)

object Report {
  implicit val format: Format[Report] = Json.format
}