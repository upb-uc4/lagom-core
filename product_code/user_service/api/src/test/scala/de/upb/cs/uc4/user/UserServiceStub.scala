package de.upb.cs.uc4.user

import akka.util.ByteString
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import de.upb.cs.uc4.authentication.model.JsonUsername
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.post.PostMessageUser
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, JsonRole, MatriculationUpdate, Usernames }

import scala.concurrent.Future

class UserServiceStub extends UserService with DefaultTestUsers {

  private var users: Seq[User] = Seq()

  def resetToDefaults(): Unit = {
    users = Seq(student0, student1, student2, lecturer0, lecturer1, lecturer2, admin0, admin1, admin2)
  }
  def resetToEmpty(): Unit = {
    users = Seq()
  }

  override def getAllUsers(usernames: Option[String]): ServiceCall[NotUsed, GetAllUsersResponse] = ServiceCall { _ => Future.successful(null) }

  override def deleteUser(username: String): ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAllStudents(usernames: Option[String]): ServiceCall[NotUsed, Seq[Student]] = ServiceCall { _ => Future.successful(null) }

  override def getUser(username: String): ServiceCall[NotUsed, User] = ServiceCall { _ =>
    val optUsers = users.find(_.username == username)

    if (optUsers.isDefined) {
      Future.successful(optUsers.get)
    }
    else {
      Future.failed(CustomException.NotFound)
    }
  }

  override def updateUser(username: String): ServiceCall[User, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAllLecturers(usernames: Option[String]): ServiceCall[NotUsed, Seq[Lecturer]] = ServiceCall { _ => Future.successful(null) }

  override def getAllAdmins(usernames: Option[String]): ServiceCall[NotUsed, Seq[Admin]] = ServiceCall { _ => Future.successful(null) }

  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] = ServiceCall { _ => Future.successful(null) }

  override def allowedGetPost: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedDeleteGetPut: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def userCreationTopic(): Topic[Usernames] = null

  override def userDeletedTopic(): Topic[JsonUsername] = null

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done] = ServiceCall { _ => Future.successful(Done) }

  override def addUser(): ServiceCall[PostMessageUser, User] = ServiceCall { _ => Future.successful(null) }

  override def getImage(username: String): ServiceCall[NotUsed, ByteString] = ServiceCall { _ => Future.successful(null) }

  override def setImage(username: String): ServiceCall[Array[Byte], Done] = ServiceCall { _ => Future.successful(Done) }

  override def deleteImage(username: String): ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }
}
