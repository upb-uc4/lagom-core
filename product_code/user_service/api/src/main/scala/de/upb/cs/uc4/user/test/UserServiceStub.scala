package de.upb.cs.uc4.user.test

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent, PostMessageUser }
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, JsonRole, JsonUsername, MatriculationUpdate }

import scala.concurrent.Future

class UserServiceStub extends UserService with DefaultTestUsers {

  private var students: Seq[Student] = Seq()

  def resetToDefaults(): Unit = {
    students = Seq(student0, student1, student2)
  }
  def resetToEmpty(): Unit = {
    students = Seq()
  }

  override def getAllUsers(usernames: Option[String]): ServiceCall[NotUsed, GetAllUsersResponse] = ServiceCall { _ => Future.successful(null) }

  override def deleteUser(username: String): ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAllStudents(usernames: Option[String]): ServiceCall[NotUsed, Seq[Student]] = ServiceCall { _ => Future.successful(null) }

  /*override def getStudent(username: String): ServiceCall[NotUsed, Student] = ServiceCall { _ =>
    val optStudent = students.find(_.username == username)

    if (optStudent.isDefined) {
      Future.successful(optStudent.get)
    }
    else {
      throw CustomException.NotFound
    }
  }*/

  override def getUser(username: String): ServiceCall[NotUsed, User] = ServiceCall { _ =>
    val optUsers = students.find(_.username == username)

    if (optUsers.isDefined) {
      Future.successful(optUsers.get)
    }
    else {
      throw CustomException.NotFound
    }
  }

  override def updateStudent(username: String): ServiceCall[Student, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAllLecturers(usernames: Option[String]): ServiceCall[NotUsed, Seq[Lecturer]] = ServiceCall { _ => Future.successful(null) }

  //override def getLecturer(username: String): ServiceCall[NotUsed, Lecturer] = ServiceCall { _ => Future.successful(null) }

  override def updateLecturer(username: String): ServiceCall[Lecturer, Done] = ServiceCall { _ => Future.successful(Done) }

  override def getAllAdmins(usernames: Option[String]): ServiceCall[NotUsed, Seq[Admin]] = ServiceCall { _ => Future.successful(null) }

  //override def getAdmin(username: String): ServiceCall[NotUsed, Admin] = ServiceCall { _ => Future.successful(null) }

  override def updateAdmin(username: String): ServiceCall[Admin, Done] = ServiceCall { _ => Future.successful(null) }

  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] = ServiceCall { _ => Future.successful(null) }

  override def allowedGetPut: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(null) }

  override def allowedGetPost: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedGet: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def allowedDelete: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def userDeletedTopic(): Topic[JsonUsername] = null

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall { _ => Future.successful(Done) }

  override def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done] = ServiceCall { _ => Future.successful(Done) }

  override def addUser(): ServiceCall[PostMessageUser, User] = { _ => Future.successful(null) }
}
