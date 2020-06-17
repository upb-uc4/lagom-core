/**
 * UC4
 * This is the API for UC4.
 *
 * OpenAPI spec version: 0.0.1
 * Contact: apiteam@uc4.cs.upb.de
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package de.upb.cs.uc4.course.model

import play.api.libs.json._

case class Course (
                  /*courseId: Option[Long],
                  name: Option[String],
                  startDate: Option[DateTime],
                  endDate: Option[DateTime],
                  lecturerId: Option[Long],
                  maxStudents: Option[Long]*/

                  courseId: String,
                  courseName: String,
                  courseType: String,
                  startDate: String,
                  endDate: String,
                  ects: Int,
                  lecturerId: String,
                  maxParticipants: Int,
                  currentParticipants : Int,
                  courseLanguage: String,
                  courseDescription: String
)

object Course {
  implicit val format: Format[Course] = Json.format

}

