package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.user.model.{Address, Role}
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, JsResult, JsValue, Json}

trait User {
  val username: String
  val role: Role
  val address: Address
  val firstName: String
  val lastName: String
  val picture: String
  val email: String
  val birthDate: String


  def trim: User
}

object User {
  implicit val format: Format[User] = new Format[User] {
    override def reads(json: JsValue): JsResult[User] = {
      json("role").as[Role] match {
        case Role.Admin => Json.fromJson[Admin](json)
        case Role.Student => Json.fromJson[Student](json)
        case Role.Lecturer => Json.fromJson[Lecturer](json)
      }
    }

    override def writes(o: User): JsValue = {
      o match {
        case admin: Admin => Json.toJson(admin)
        case student: Student => Json.toJson(student)
        case lecturer: Lecturer => Json.toJson(lecturer)
      }
    }
  }
}