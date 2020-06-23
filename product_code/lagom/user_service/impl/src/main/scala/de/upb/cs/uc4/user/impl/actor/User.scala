package de.upb.cs.uc4.user.impl.actor

import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.user.{Admin, Lecturer, Student}
import play.api.libs.json.{Format, Json}

case class User(optStudent: Option[Student], optLecturer: Option[Lecturer], optAdmin: Option[Admin], role: Role) {

  def student: Student = optStudent.get
  def lecturer: Lecturer = optLecturer.get
  def admin: Admin = optAdmin.get

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

  def apply(student: Student) = new User(Some(student), None, None, Role.Student)

  def apply(lecturer: Lecturer) = new User(None, Some(lecturer), None, Role.Lecturer)

  def apply(admin: Admin) = new User(None, None, Some(admin), Role.Admin)
}