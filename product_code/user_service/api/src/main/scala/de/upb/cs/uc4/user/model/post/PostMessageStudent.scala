package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.{ CustomException, DetailedError, SimpleError }
import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.user.Student
import play.api.libs.json.{ Format, Json }

case class PostMessageStudent(authUser: AuthenticationUser, student: Student) extends PostMessageUser {

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser
  ): PostMessageStudent = copy(authUser, student)

  override def validate: Seq[SimpleError] = {
    var errors: List[SimpleError] = student.validate.map {
      simpleError =>
        SimpleError("student." + simpleError.name, simpleError.reason)
    }.toList
    errors ++= super.validate

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

