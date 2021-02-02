package de.upb.cs.uc4.admission.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.upb.cs.uc4.admission.model.AdmissionType.AdmissionType
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, JsResult, JsValue, Json }

import scala.concurrent.{ ExecutionContext, Future }

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
trait AbstractAdmission {
  val admissionId: String
  val enrollmentId: String
  val timestamp: String
  val `type`: String

  def copyAdmission(
      admissionId: String = this.admissionId,
      enrollmentId: String = this.enrollmentId,
      timestamp: String = this.timestamp,
      `type`: String = this.`type`
  ): AbstractAdmission

  def trim: AbstractAdmission = copyAdmission(admissionId.trim, enrollmentId.trim, timestamp.trim, `type`.trim)

  def validateOnCreation(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    var errors = List[SimpleError]()

    if (admissionId.nonEmpty) {
      errors :+= SimpleError("admissionId", "AdmissionId must be empty.")
    }
    if (enrollmentId.nonEmpty) {
      errors :+= SimpleError("enrollmentId", "EnrollmentId must be empty.")
    }
    if (timestamp.nonEmpty) {
      errors :+= SimpleError("timestamp", "Timestamp must be empty.")
    }
    errors
  }
}

object AbstractAdmission {
  implicit val format: Format[AbstractAdmission] = new Format[AbstractAdmission] {
    override def reads(json: JsValue): JsResult[AbstractAdmission] = {
      json("type").as[AdmissionType] match {
        case AdmissionType.Course => Json.fromJson[CourseAdmission](json)
        case AdmissionType.Exam   => Json.fromJson[ExamAdmission](json)
      }
    }

    override def writes(o: AbstractAdmission): JsValue = {
      o match {
        case courseAdmission: CourseAdmission => Json.toJson(courseAdmission)
        case examAdmission: ExamAdmission     => Json.toJson(examAdmission)
      }
    }
  }
}
