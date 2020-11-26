package de.upb.cs.uc4.user.api

import akka.util.ByteString
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.Service.topic
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceAcl, ServiceCall }
import de.upb.cs.uc4.shared.client.UC4Service
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.client.message_serialization.CustomMessageSerializer
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student, User }
import de.upb.cs.uc4.user.model._

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

  val enrollmentIdSecretByteLength: Int = 15

  // USER
  /** Get a specific user with the username */
  def getUser(username: String): ServiceCall[NotUsed, User]

  /** Get all users from the database */
  def getAllUsers(usernames: Option[String], only_active: Option[Boolean]): ServiceCall[NotUsed, GetAllUsersResponse]

  /** Adds the contents of the postMessageUser, authUser to authentication users and user to users */
  def addUser(): ServiceCall[PostMessageUser, User]

  /** Update an existing user */
  def updateUser(username: String): ServiceCall[User, Done]

  /** Completely deletes a users from the database */
  def forceDeleteUser(username: String): ServiceCall[NotUsed, Done]

  /** Flags a user as deleted and deletes (not required) personal info */
  def softDeleteUser(username: String): ServiceCall[NotUsed, Done]

  // STUDENT
  /** Get all students from the database */
  def getAllStudents(usernames: Option[String], only_active: Option[Boolean]): ServiceCall[NotUsed, Seq[Student]]

  // LECTURER
  /** Get all lecturers from the database */
  def getAllLecturers(usernames: Option[String], only_active: Option[Boolean]): ServiceCall[NotUsed, Seq[Lecturer]]

  // ADMIN
  /** Get all admins from the database */
  def getAllAdmins(usernames: Option[String], only_active: Option[Boolean]): ServiceCall[NotUsed, Seq[Admin]]

  // ROLE
  def getRole(username: String): ServiceCall[NotUsed, JsonRole]

  /** Update latestMatriculation */
  def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done]

  /** Gets the image of the user */
  def getImage(username: String): ServiceCall[NotUsed, ByteString]

  /** Gets the thumbnail of the user */
  def getThumbnail(username: String): ServiceCall[NotUsed, ByteString]

  /** Sets the image of the user */
  def setImage(username: String): ServiceCall[Array[Byte], Done]

  /** Delete the image of the user */
  def deleteImage(username: String): ServiceCall[NotUsed, Done]

  // OPTIONS
  /** Allows GET, POST */
  def allowedGetPost: ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows DELETE, GET, PUT */
  def allowedDeleteGetPut: ServiceCall[NotUsed, Done]

  /** Allows DELETE */
  def allowedDelete: ServiceCall[NotUsed, Done]

  /** Publishes every new user */
  def userCreationTopic(): Topic[EncryptionContainer]

  /** Publishes every deletion of a user, sending the username*/
  def userDeletionTopicMinimal(): Topic[EncryptionContainer]

  /** Publishes every deletion of a user, sending the username and role */
  def userDeletionTopicPrecise(): Topic[EncryptionContainer]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/users?usernames&only_active", getAllUsers _),
        restCall(Method.POST, pathPrefix + "/users", addUser _)(CustomMessageSerializer.jsValueFormatMessageSerializer, CustomMessageSerializer.jsValueFormatMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/users", allowedGetPost _),

        restCall(Method.GET, pathPrefix + "/users/:username", getUser _),
        restCall(Method.DELETE, pathPrefix + "/users/:username", softDeleteUser _),
        restCall(Method.PUT, pathPrefix + "/users/:username", updateUser _)(CustomMessageSerializer.jsValueFormatMessageSerializer, MessageSerializer.DoneMessageSerializer),
        restCall(Method.OPTIONS, pathPrefix + "/users/:username", allowedDeleteGetPut _),

        restCall(Method.DELETE, pathPrefix + "/users/:username/force", forceDeleteUser _),
        restCall(Method.OPTIONS, pathPrefix + "/users/:username/force", allowedDelete _),

        restCall(Method.GET, pathPrefix + "/users/:username/role", getRole _),
        restCall(Method.OPTIONS, pathPrefix + "/users/:username/role", allowedGet _),

        restCall(Method.GET, pathPrefix + "/students?usernames&only_active", getAllStudents _),
        restCall(Method.OPTIONS, pathPrefix + "/students", allowedGet _),

        restCall(Method.GET, pathPrefix + "/lecturers?usernames&only_active", getAllLecturers _),
        restCall(Method.OPTIONS, pathPrefix + "/lecturers", allowedGet _),

        restCall(Method.GET, pathPrefix + "/admins?usernames&only_active", getAllAdmins _),
        restCall(Method.OPTIONS, pathPrefix + "/admins", allowedGet _),

        restCall(Method.GET, pathPrefix + "/users/:username/image", getImage _)(MessageSerializer.NotUsedMessageSerializer, MessageSerializer.NoopMessageSerializer),
        restCall(Method.GET, pathPrefix + "/users/:username/thumbnail", getThumbnail _)(MessageSerializer.NotUsedMessageSerializer, MessageSerializer.NoopMessageSerializer),
        restCall(Method.DELETE, pathPrefix + "/users/:username/image", deleteImage _),

        restCall(Method.OPTIONS, pathPrefix + "/users/:username/image", allowedDeleteGetPut _),
        restCall(Method.OPTIONS, pathPrefix + "/users/:username/thumbnail", allowedGet _),

        //Not exposed
        restCall(Method.PUT, pathPrefix + "/matriculation", updateLatestMatriculation _),
        restCall(Method.PUT, pathPrefix + "/image/:username", setImage _)
      )
      .addAcls(
        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/users\\E" + "(\\?([^\\/\\?]+))?"),
        ServiceAcl.forMethodAndPathRegex(Method.POST, "\\Q" + pathPrefix + "/users\\E"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users\\E" + "(\\?([^\\/\\?]+))?"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.DELETE, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.PUT, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)"),

        ServiceAcl.forMethodAndPathRegex(Method.DELETE, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + "\\Q" + "force\\E"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + "\\Q" + "force\\E"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + """\/role"""),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + """\/role"""),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/students\\E" + "(\\?([^\\/\\?]+))?"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/students\\E" + "(\\?([^\\/\\?]+))?"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/lecturers\\E" + "(\\?([^\\/\\?]+))?"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/lecturers\\E" + "(\\?([^\\/\\?]+))?"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/admins\\E" + "(\\?([^\\/\\?]+))?"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/admins\\E" + "(\\?([^\\/\\?]+))?"),

        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + """\/image"""),
        ServiceAcl.forMethodAndPathRegex(Method.PUT, "\\Q" + pathPrefix + "/users\\E" + "([^/]+)" + """\/image"""),
        ServiceAcl.forMethodAndPathRegex(Method.DELETE, "\\Q" + pathPrefix + "/users\\E" + "([^/]+)" + """\/image"""),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + """\/image"""),
        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + """\/thumbnail"""),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/users/\\E" + "([^/]+)" + """\/thumbnail""")
      )
      .addTopics(
        topic(UserService.ADD_TOPIC_NAME, userCreationTopic _),
        topic(UserService.DELETE_TOPIC_MINIMAL_NAME, userDeletionTopicMinimal _),
        topic(UserService.DELETE_TOPIC_PRECISE_NAME, userDeletionTopicPrecise _)
      )
  }
}

object UserService {
  val ADD_TOPIC_NAME = "add"
  val DELETE_TOPIC_MINIMAL_NAME = "deleteUserMinimal"
  val DELETE_TOPIC_PRECISE_NAME = "deleteUserPrecise"
}
