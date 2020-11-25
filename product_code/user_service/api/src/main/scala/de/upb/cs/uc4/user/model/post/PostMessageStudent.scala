package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.user.Student
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class PostMessageStudent(authUser: AuthenticationUser, governmentId: String, student: Student) extends PostMessageUser {

  override val userString: String = "student"

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser,
      governmentId: String = this.governmentId
  ): PostMessageStudent = copy(authUser, governmentId, student)

  override def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    student.validate.flatMap { studentErrors =>
      var errors = studentErrors.map {
        simpleError =>
          SimpleError("student." + simpleError.name, simpleError.reason)
      }

      super.validate.map { superErrors =>
        errors ++= superErrors

        //Filter username errors if authUsername and username are not equal + return error that usernames are not equal
        if (authUser.username != student.username) {
          errors.filter(simpleError => !simpleError.name.contains("username"))
          errors :+= SimpleError("authUser.username", "Username in authUser must match username in student")
          errors :+= SimpleError("student.username", "Username in student must match username in authUser")
        }

        //A student cannot be created with latestImmatriculation already set
        if (student.latestImmatriculation != "") {
          errors.filter(simpleError => !simpleError.name.contains("student.latestImmatriculation"))
          errors :+= SimpleError("student.latestImmatriculation", "Latest Immatriculation must not be set upon creation.")
        }

        errors
      }
    }
  }

  override def trim: PostMessageStudent =
    super.trim.asInstanceOf[PostMessageStudent].copy(
      student = student.trim
    )

  override def clean: PostMessageStudent =
    super.clean.asInstanceOf[PostMessageStudent].copy(
      student = student.clean
    )
}

object PostMessageStudent {
  implicit val format: Format[PostMessageStudent] = Json.format
}

