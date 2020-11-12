package de.upb.cs.uc4.user.model

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class PostMessageUser(authUser: AuthenticationUser, user: User) {

  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    authUser.validate.map {
      _.map {
        simpleError =>
          SimpleError("authUser." + simpleError.name, simpleError.reason)
      }
    }.flatMap { authUserErrors =>
      var errors = authUserErrors.toList

      user.validateOnCreation.map { userErrors =>
        errors ++= userErrors.map {
          simpleError =>
            SimpleError("user." + simpleError.name, simpleError.reason)
        }

        //Filter username errors if authUsername and username are not equal + return error that usernames are not equal
        if (authUser.username != user.username) {
          errors = errors.filter(simpleError => !simpleError.name.contains("username"))
          errors :+= SimpleError("authUser.username", "Username in authUser must match username in user")
          errors :+= SimpleError("user.username", "Username in student must match username in authUser")
        }
        errors
      }
    }
  }

  def trim: PostMessageUser = {
    copy(authUser.trim, user.trim)
  }

  def clean: PostMessageUser = {
    copy(authUser.clean, user.clean)
  }
}

object PostMessageUser {
  implicit val format: Format[PostMessageUser] = Json.format
}