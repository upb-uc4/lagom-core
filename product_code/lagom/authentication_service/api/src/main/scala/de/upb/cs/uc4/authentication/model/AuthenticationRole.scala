package de.upb.cs.uc4.authentication.model

import play.api.libs.json.{Format, Json}

object AuthenticationRole extends Enumeration {
  type AuthenticationRole = Value
  val Admin, Student, Lecturer = Value

  implicit val format: Format[AuthenticationRole] = Json.formatEnum(this)

  def All: Seq[AuthenticationRole] = values.toSeq
}
