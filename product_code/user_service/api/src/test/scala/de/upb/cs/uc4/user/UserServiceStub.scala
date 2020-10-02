package de.upb.cs.uc4.user

import akka.util.ByteString
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import de.upb.cs.uc4.authentication.model.JsonUsername
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.post.PostMessageUser
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, JsonRole, MatriculationUpdate, Role }

import scala.concurrent.Future

class UserServiceStub extends UserService with DefaultTestUsers {

  private var users: Seq[User] = Seq()

  def resetToDefaults(): Unit = {
    users = Seq(student0, student1, student2, lecturer0, lecturer1, lecturer2, admin0, admin1, admin2)
  }
  def resetToEmpty(): Unit = {
    users = Seq()
  }

  override def getAllUsers(usernames: Option[String]): ServiceCall[NotUsed, GetAllUsersResponse] = ServiceCall { _ =>
    val response = GetAllUsersResponse(
      users.filter(_.role == Role.Student).map(_.asInstanceOf[Student]),
      users.filter(_.role == Role.Lecturer).map(_.asInstanceOf[Lecturer]),
      users.filter(_.role == Role.Admin).map(_.asInstanceOf[Admin])
    )
    Future.successful(response)
  }

  override def deleteUser(username: String): ServiceCall[NotUsed, Done] = ServiceCall {
    _ =>
      users = users.filter(_.username != username)
      Future.successful(Done)
  }

  override def getAllStudents(usernames: Option[String]): ServiceCall[NotUsed, Seq[Student]] = ServiceCall { _ =>
    Future.successful(users.filter(_.role == Role.Student).map(_.asInstanceOf[Student]))
  }

  override def getUser(username: String): ServiceCall[NotUsed, User] = ServiceCall { _ =>
    val optUsers = users.find(_.username == username)

    if (optUsers.isDefined) {
      Future.successful(optUsers.get)
    }
    else {
      Future.failed(UC4Exception.NotFound)
    }
  }

  override def updateUser(username: String): ServiceCall[User, Done] = ServiceCall { updatedUser =>
    val optUser = users.find(_.username == username)
    optUser match {
      case Some(user) =>
        users = users.filter(_.username != username)
        users :+= updatedUser
        Future.successful(Done)
      case None =>
        Future.failed(UC4Exception.NotFound)
    }
  }

  override def getAllLecturers(usernames: Option[String]): ServiceCall[NotUsed, Seq[Lecturer]] = ServiceCall { _ =>
    Future.successful(users.filter(_.role == Role.Lecturer).map(_.asInstanceOf[Lecturer]))
  }

  override def getAllAdmins(usernames: Option[String]): ServiceCall[NotUsed, Seq[Admin]] = ServiceCall { _ =>
    Future.successful(users.filter(_.role == Role.Admin).map(_.asInstanceOf[Admin]))
  }

  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] = ServiceCall { _ =>
    val optUser = users.find(_.username == username)
    optUser match {
      case Some(user) => Future.successful(JsonRole(user.role))
      case None =>Future.failed(UC4Exception.NotFound)
    }
  }

  override def allowedGetPost: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedDeleteGetPut: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def userDeletedTopic(): Topic[JsonUsername] = null

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done] = ServiceCall { _ => Future.successful(Done) }

  override def addUser(): ServiceCall[PostMessageUser, User] = ServiceCall { pmu =>
    users ++= Seq(pmu.getUser)
    Future.successful(pmu.getUser)
  }

  override def getImage(username: String): ServiceCall[NotUsed, ByteString] = ServiceCall { _ => Future.successful(null) }

  override def setImage(username: String): ServiceCall[Array[Byte], Done] = ServiceCall { _ => Future.successful(Done) }

  override def deleteImage(username: String): ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }
}
