package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.{ SimpleError, UC4Exception }
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
      json match {
        case json if (json \ "student").isDefined => Json.fromJson[PostMessageStudent](json)
        case json if (json \ "lecturer").isDefined => Json.fromJson[PostMessageLecturer](json)
        case json if (json \ "admin").isDefined => Json.fromJson[PostMessageAdmin](json)
        case _ => throw UC4Exception.DeserializationError
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