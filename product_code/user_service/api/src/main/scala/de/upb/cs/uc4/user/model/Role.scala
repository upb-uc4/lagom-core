package de.upb.cs.uc4.user.model

import play.api.libs.json.{ Format, Json }

object Role extends Enumeration {
  type Role = Value
  val Admin, Student, Lecturer = Value

  implicit val format: Format[Role] = Json.formatEnum(this)

  def All: Seq[Role] = values.toSeq
}
