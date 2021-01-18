package de.upb.cs.uc4.user

import akka.util.ByteString
import akka.{ Done, NotUsed }
import com.google.common.io.{ BaseEncoding, ByteStreams }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import de.upb.cs.uc4.user.model.{ PostMessageUser, _ }

import scala.concurrent.Future
import scala.util.Random

class UserServiceStub extends UserService with DefaultTestUsers {

  private lazy val defaultThumbnail = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultThumbnail.jpg"))

  private var users: Seq[User] = Seq()

  def resetToDefaults(): Unit = {
    users = Seq(student0, student1, student2, lecturer0, lecturer1, lecturer2, admin0, admin1, admin2)
  }
  def resetToEmpty(): Unit = {
    users = Seq()
  }

  override def getAllUsers(usernames: Option[String], isActive: Option[Boolean]): ServiceCall[NotUsed, GetAllUsersResponse] = ServiceCall { _ =>
    val response = GetAllUsersResponse(
      users.filter(_.role == Role.Student).map(_.asInstanceOf[Student]),
      users.filter(_.role == Role.Lecturer).map(_.asInstanceOf[Lecturer]),
      users.filter(_.role == Role.Admin).map(_.asInstanceOf[Admin])
    )
    Future.successful(response)
  }

  override def forceDeleteUser(username: String): ServiceCall[NotUsed, Done] = ServiceCall {
    _ =>
      users = users.filter(_.username != username)
      Future.successful(Done)
  }

  override def getAllStudents(usernames: Option[String], isActive: Option[Boolean]): ServiceCall[NotUsed, Seq[Student]] = ServiceCall { _ =>
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
      case Some(_) =>
        users = users.filter(_.username != username)
        users :+= updatedUser
        Future.successful(Done)
      case None =>
        Future.failed(UC4Exception.NotFound)
    }
  }

  override def getAllLecturers(usernames: Option[String], isActive: Option[Boolean]): ServiceCall[NotUsed, Seq[Lecturer]] = ServiceCall { _ =>
    Future.successful(users.filter(_.role == Role.Lecturer).map(_.asInstanceOf[Lecturer]))
  }

  override def getAllAdmins(usernames: Option[String], isActive: Option[Boolean]): ServiceCall[NotUsed, Seq[Admin]] = ServiceCall { _ =>
    Future.successful(users.filter(_.role == Role.Admin).map(_.asInstanceOf[Admin]))
  }

  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] = ServiceCall { _ =>
    val optUser = users.find(_.username == username)
    optUser match {
      case Some(user) => Future.successful(JsonRole(user.role))
      case None       => Future.failed(UC4Exception.NotFound)
    }
  }

  override def allowedGetPost: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedDeleteGetPut: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedDelete: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def userCreationTopic(): Topic[EncryptionContainer] = null //EncryptionContainer[Usernames]

  override def userDeletionTopicMinimal(): Topic[EncryptionContainer] = null //EncryptionContainer[JsonUsername]

  override def userDeletionTopicPrecise(): Topic[EncryptionContainer] = null //EncryptionContainer[JsonUserData]

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done] = ServiceCall { _ => Future.successful(Done) }

  override def addUser(): ServiceCall[PostMessageUser, User] = ServiceCall { pmu =>
    val rnd = new Random
    val bytes = new Array[Byte](enrollmentIdSecretByteLength)
    rnd.nextBytes(bytes)
    users ++= Seq(pmu.user.copyUser(enrollmentIdSecret = BaseEncoding.base64().encode(bytes)))
    Future.successful(pmu.user)
  }

  override def getImage(username: String): ServiceCall[NotUsed, ByteString] = ServiceCall { _ => Future.successful(ByteString(defaultThumbnail)) }

  override def getThumbnail(username: String): ServiceCall[NotUsed, ByteString] = ServiceCall { _ => Future.successful(ByteString(defaultThumbnail)) }

  override def setImage(username: String): ServiceCall[Array[Byte], Done] = ServiceCall { _ => Future.successful(Done) }

  override def deleteImage(username: String): ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  /** Flags a user as deleted and deletes personal info from now on unrequired */
  override def softDeleteUser(username: String): ServiceCall[NotUsed, Done] = ServiceCall {
    _ =>
      val optUser = users.find(_.username == username)
      if (optUser.isEmpty) {
        Future.failed(UC4Exception.NotFound)
      }
      else {
        users = users.filter(_.username != username) :+ optUser.get.softDelete
        Future.successful(Done)
      }
  }
}
