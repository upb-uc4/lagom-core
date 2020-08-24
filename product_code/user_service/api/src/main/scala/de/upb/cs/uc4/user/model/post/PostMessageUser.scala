package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{ Format, JsResult, JsValue, Json }

trait PostMessageUser {
  val authUser: AuthenticationUser

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser
  ): PostMessageUser

  def getUser: User = {
    this match {
      case postStudent: PostMessageStudent   => postStudent.student
      case postLecturer: PostMessageLecturer => postLecturer.lecturer
      case postAdmin: PostMessageAdmin       => postAdmin.admin
    }
  }

  def trim: PostMessageUser = {
    copyPostMessageUser(authUser.trim)
  }
  def clean: PostMessageUser = {
    trim.copyPostMessageUser(authUser.clean)
  }
}

object PostMessageUser {
  implicit val format: Format[PostMessageUser] = new Format[PostMessageUser] {
    override def reads(json: JsValue): JsResult[PostMessageUser] = {
      json match {
        case json if (json \ "student").isDefined => Json.fromJson[PostMessageStudent](json)
        case json if (json \ "lecturer").isDefined => Json.fromJson[PostMessageLecturer](json)
        case json if (json \ "admin").isDefined => Json.fromJson[PostMessageAdmin](json)
        case _ => throw CustomException.DeserializationError
      }
    }

    override def writes(o: PostMessageUser): JsValue = {
      o match {
        case student: PostMessageStudent   => Json.toJson(student)
        case lecturer: PostMessageLecturer => Json.toJson(lecturer)
        case admin: PostMessageAdmin       => Json.toJson(admin)
      }
    }
  }
}