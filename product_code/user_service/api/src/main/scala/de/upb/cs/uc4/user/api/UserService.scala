package de.upb.cs.uc4.user.api

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceAcl, ServiceCall }
import de.upb.cs.uc4.authentication.model.JsonUsername
import de.upb.cs.uc4.shared.client.UC4Service
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer
import de.upb.cs.uc4.user.model.post.PostMessageUser
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
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
  override val autoAcl: Boolean = false

  // USER
  /** Get a specific user with the username */
  def getUser(username: String): ServiceCall[NotUsed, User]

  /** Get all users from the database */
  def getAllUsers(usernames: Option[String]): ServiceCall[NotUsed, GetAllUsersResponse]

  /** Adds the contents of the postMessageUser, authUser to authentication users and user to users */
  def addUser(): ServiceCall[PostMessageUser, User]

  /** Update an existing user */
  def updateUser(username: String): ServiceCall[User, Done]

  /** Delete a user from the database */
  def deleteUser(username: String): ServiceCall[NotUsed, Done]

  // STUDENT
  /** Get all students from the database */
  def getAllStudents(usernames: Option[String]): ServiceCall[NotUsed, Seq[Student]]

  // LECTURER
  /** Get all lecturers from the database */
  def getAllLecturers(usernames: Option[String]): ServiceCall[NotUsed, Seq[Lecturer]]

  // ADMIN
  /** Get all admins from the database */
  def getAllAdmins(usernames: Option[String]): ServiceCall[NotUsed, Seq[Admin]]

  // ROLE
  def getRole(username: String): ServiceCall[NotUsed, JsonRole]

  /** Update latestMatriculation */
  def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done]

  // OPTIONS
  /** Allows GET, POST */
  def allowedGetPost: ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows DELETE, GET, PUT */
  def allowedDeleteGetPut: ServiceCall[NotUsed, Done]

  /** Publishes every deletion of a user */
  def userDeletedTopic(): Topic[JsonUsername]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/users?usernames", getAllUsers _),
        restCall(Method.POST, pathPrefix + "/users", addUser _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/users", allowedGetPost _),

        restCall(Method.GET, pathPrefix + "/users/:username", getUser _),
        restCall(Method.DELETE, pathPrefix + "/users/:username", deleteUser _),
        restCall(Method.PUT, pathPrefix + "/users/:username", updateUser _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/users/:username", allowedDeleteGetPut _),

        restCall(Method.GET, pathPrefix + "/users/:username/role", getRole _),
        restCall(Method.OPTIONS, pathPrefix + "/users/:username/role", allowedGet _),

        restCall(Method.GET, pathPrefix + "/students?usernames", getAllStudents _),
        restCall(Method.OPTIONS, pathPrefix + "/students", allowedGet _),

        restCall(Method.GET, pathPrefix + "/lecturers?usernames", getAllLecturers _),
        restCall(Method.OPTIONS, pathPrefix + "/lecturers", allowedGet _),

        restCall(Method.GET, pathPrefix + "/admins?usernames", getAllAdmins _),
        restCall(Method.OPTIONS, pathPrefix + "/admins", allowedGet _),

        //Not exposed
        restCall(Method.PUT, pathPrefix + "/matriculation", updateLatestMatriculation _)
      )
      .addAcls(
        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/users\\E" + "(\\?([^\\/\\?]+))?"),
        ServiceAcl.forMethodAndPathRegex(Method.POST, "\\Q" + pathPrefix + "/users\\E"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users\\E" + "(\\?([^\\/\\?]+))?"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.DELETE, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.PUT, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + """\/role"""),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + """\/role"""),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/students\\E" + "(\\?([^\\/\\?]+))?"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/students\\E" + "(\\?([^\\/\\?]+))?"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/lecturers\\E" + "(\\?([^\\/\\?]+))?"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/lecturers\\E" + "(\\?([^\\/\\?]+))?"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/admins\\E" + "(\\?([^\\/\\?]+))?"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/admins\\E" + "(\\?([^\\/\\?]+))?")
      )
      .withTopics(
        topic(UserService.DELETE_TOPIC_NAME, userDeletedTopic _)
      )

  }
}

object UserService {
  val DELETE_TOPIC_NAME = "delete"
}