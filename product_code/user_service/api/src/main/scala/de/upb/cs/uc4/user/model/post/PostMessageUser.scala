package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{ Format, JsResult, JsValue, Json }

import scala.concurrent.{ ExecutionContext, Future }

trait PostMessageUser {
  val authUser: AuthenticationUser
  val user: User

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser,
      user: User = this.user
  ): PostMessageUser

  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    authUser.validate.map {
      _.map {
        simpleError =>
          SimpleError("authUser." + simpleError.name, simpleError.reason)
      }
    }
  }

  def trim: PostMessageUser = {
    copyPostMessageUser(authUser.trim, user.trim)
  }

  def clean: PostMessageUser = {
    trim.copyPostMessageUser(authUser.clean, user.clean)
  }
}

object PostMessageUser {
  implicit val format: Format[PostMessageUser] = new Format[PostMessageUser] {
    override def reads(json: JsValue): JsResult[PostMessageUser] = {
      json("user")("role").as[Role] match {
        case Role.Student  => Json.fromJson[PostMessageStudent](json)
        case Role.Lecturer => Json.fromJson[PostMessageLecturer](json)
        case Role.Admin    => Json.fromJson[PostMessageAdmin](json)
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