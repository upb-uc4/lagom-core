package de.upb.cs.uc4.user.impl.actor

import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.user.{Admin, Lecturer, Student}
import play.api.libs.json.{Format, Json}

case class User(student: Student, lecturer: Lecturer, admin: Admin, role: Role) {

  def getUsername: String = {
    role match {
      case Role.Student => student.username
      case Role.Lecturer => lecturer.username
      case Role.Admin => admin.username
    }
  }
}

object User{
  implicit val format: Format[User] = Json.format

  def apply(student: Student) = new User(student, null, null, Role.Student)

  def apply(lecturer: Lecturer) = new User(null, lecturer, null, Role.Lecturer)

  def apply(admin: Admin) = new User(null, null, admin, Role.Admin)
}