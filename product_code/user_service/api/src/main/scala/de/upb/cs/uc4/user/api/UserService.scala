package de.upb.cs.uc4.user.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.authentication.model.JsonUsername
import de.upb.cs.uc4.shared.client.UC4Service
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer
import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent }
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student }
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, JsonRole, MatriculationUpdate }

/** The UserService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the UserService.
  */
trait UserService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix = "/user-management"
  /** The name of the service */
  override val name = "user"

  // USER

  /** Get all users from the database */
  def getAllUsers(usernames: Option[String]): ServiceCall[NotUsed, GetAllUsersResponse]

  /** Delete a users from the database */
  def deleteUser(username: String): ServiceCall[NotUsed, Done]

  // STUDENT

  /** Get all students from the database */
  def getAllStudents(usernames: Option[String]): ServiceCall[NotUsed, Seq[Student]]

  /** Add a new student to the database */
  def addStudent(): ServiceCall[PostMessageStudent, Student]

  /** Get a specific student */
  def getStudent(username: String): ServiceCall[NotUsed, Student]

  /** Update an existing student */
  def updateStudent(username: String): ServiceCall[Student, Done]

  // LECTURER

  /** Get all lecturers from the database */
  def getAllLecturers(usernames: Option[String]): ServiceCall[NotUsed, Seq[Lecturer]]

  /** Add a new lecturer to the database */
  def addLecturer(): ServiceCall[PostMessageLecturer, Lecturer]

  /** Get a specific lecturer */
  def getLecturer(username: String): ServiceCall[NotUsed, Lecturer]

  /** Update an existing lecturer */
  def updateLecturer(username: String): ServiceCall[Lecturer, Done]

  // ADMIN

  /** Get all admins from the database */
  def getAllAdmins(usernames: Option[String]): ServiceCall[NotUsed, Seq[Admin]]

  /** Add a new admin to the database */
  def addAdmin(): ServiceCall[PostMessageAdmin, Admin]

  /** Get a specific admin */
  def getAdmin(username: String): ServiceCall[NotUsed, Admin]

  /** Update an existing admin */
  def updateAdmin(username: String): ServiceCall[Admin, Done]

  // ROLE
  def getRole(username: String): ServiceCall[NotUsed, JsonRole]

  /** Update latestMatriculation */
  def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done]

  // OPTIONS

  /** Allows GET, PUT */
  def allowedGetPut: ServiceCall[NotUsed, Done]

  /** Allows GET, POST */
  def allowedGetPost: ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows DELETE */
  def allowedDelete: ServiceCall[NotUsed, Done]

  /** Publishes every deletion of a user */
  def userDeletedTopic(): Topic[JsonUsername]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/users?usernames", getAllUsers _),
        restCall(Method.DELETE, pathPrefix + "/users/:username", deleteUser _),
        restCall(Method.OPTIONS, pathPrefix + "/users", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/users/:username", allowedDelete _),

        restCall(Method.GET, pathPrefix + "/role/:username", getRole _),
        restCall(Method.OPTIONS, pathPrefix + "/role/:username", allowedGet _),

        restCall(Method.PUT, pathPrefix + "/matriculation", updateLatestMatriculation _),
        restCall(Method.GET, pathPrefix + "/students?usernames", getAllStudents _),
        restCall(Method.POST, pathPrefix + "/students", addStudent _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.GET, pathPrefix + "/students/:username", getStudent _),
        restCall(Method.PUT, pathPrefix + "/students/:username", updateStudent _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/students/:username", allowedGetPut _),
        restCall(Method.OPTIONS, pathPrefix + "/students", allowedGetPost _),

        restCall(Method.GET, pathPrefix + "/lecturers?usernames", getAllLecturers _),
        restCall(Method.POST, pathPrefix + "/lecturers", addLecturer _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.GET, pathPrefix + "/lecturers/:username", getLecturer _),
        restCall(Method.PUT, pathPrefix + "/lecturers/:username", updateLecturer _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/lecturers/:username", allowedGetPut _),
        restCall(Method.OPTIONS, pathPrefix + "/lecturers", allowedGetPost _),

        restCall(Method.GET, pathPrefix + "/admins?usernames", getAllAdmins _),
        restCall(Method.POST, pathPrefix + "/admins", addAdmin _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.GET, pathPrefix + "/admins/:username", getAdmin _),
        restCall(Method.PUT, pathPrefix + "/admins/:username", updateAdmin _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/admins/:username", allowedGetPut _),
        restCall(Method.OPTIONS, pathPrefix + "/admins", allowedGetPost _)
      )
      .withTopics(
        topic(UserService.DELETE_TOPIC_NAME, userDeletedTopic _)
      )
  }
}

object UserService {
  val DELETE_TOPIC_NAME = "delete"
}