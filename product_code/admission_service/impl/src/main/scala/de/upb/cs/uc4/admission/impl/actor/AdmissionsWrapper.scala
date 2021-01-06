package de.upb.cs.uc4.admission.impl.actor

import de.upb.cs.uc4.admission.model.CourseAdmission
import play.api.libs.json.{ Format, Json }

case class AdmissionsWrapper(admissions: Seq[CourseAdmission])

object AdmissionsWrapper {
  implicit val format: Format[AdmissionsWrapper] = Json.format
}
