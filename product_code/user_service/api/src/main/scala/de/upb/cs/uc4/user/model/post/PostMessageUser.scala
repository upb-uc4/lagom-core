package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.{ SimpleError, UC4Exception }
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import play.api.libs.json.{ Format, JsResult, JsValue, Json }

import scala.concurrent.{ ExecutionContext, Future }

trait PostMessageUser {
  val authUser: AuthenticationUser
  val governmentId: String

  val userString: String

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser,
      governmentId: String = this.governmentId
  ): PostMessageUser

  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    authUser.validate.map {
      authErrors =>
        var errorList = authErrors
        errorList.map {
          simpleError =>
            SimpleError("authUser." + simpleError.name, simpleError.reason)
        }

        if (governmentId.isEmpty) {
          errorList :+= SimpleError("governmentId", "GovernmentId must not be empty.")
        }

        if (getUser.enrollmentIdSecret.nonEmpty) {
          errorList :+= SimpleError(s"$userString.enrollmentIdSecret", "EnrollmentIdSecret must be empty.")
        }

        errorList
    }
  }

  def getUser: User = {
    this match {
      case postStudent: PostMessageStudent   => postStudent.student
      case postLecturer: PostMessageLecturer => postLecturer.lecturer
      case postAdmin: PostMessageAdmin       => postAdmin.admin
    }
  }

  def copyWithUser(user: User): PostMessageUser = {
    this match {
      case postStudent: PostMessageStudent =>
        if (!user.isInstanceOf[Student]) {
          throw UC4Exception.InternalServerError("Parser Error", "Tried to set user in PostMessageStudent to non-Student")
        }
        postStudent.copy(student = user.asInstanceOf[Student])
      case postLecturer: PostMessageLecturer =>
        if (!user.isInstanceOf[Lecturer]) {
          throw UC4Exception.InternalServerError("Parser Error", "Tried to set user in PostMessageLecturer to non-Lecturer")
        }
        postLecturer.copy(lecturer = user.asInstanceOf[Lecturer])
      case postAdmin: PostMessageAdmin =>
        if (!user.isInstanceOf[Admin]) {
          throw UC4Exception.InternalServerError("Parser Error", "Tried to set user in PostMessageAdmin to non-Admin")
        }
        postAdmin.copy(admin = user.asInstanceOf[Admin])
    }
  }

  def trim: PostMessageUser = {
    copyPostMessageUser(authUser.trim, governmentId.trim)
  }

  def clean: PostMessageUser = {
    trim.copyPostMessageUser(authUser.clean, governmentId)
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