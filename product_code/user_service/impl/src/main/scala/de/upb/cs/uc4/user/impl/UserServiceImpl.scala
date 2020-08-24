package de.upb.cs.uc4.user.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{ EventStreamElement, PersistentEntityRegistry, ReadSide }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser }
import de.upb.cs.uc4.shared.client.exceptions.{ CustomException, DetailedError, SimpleError }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected, RejectedWithError }
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.UserState
import de.upb.cs.uc4.user.impl.commands._
import de.upb.cs.uc4.user.impl.events.{ OnLatestMatriculationUpdate, OnUserDelete, UserEvent }
import de.upb.cs.uc4.user.impl.readside.{ UserDatabase, UserEventProcessor }
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent, PostMessageUser }
import de.upb.cs.uc4.user.model.user._
import de.upb.cs.uc4.user.model.{ GetAllUsersResponse, JsonRole, JsonUsername, MatriculationUpdate, Role }

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

/** Implementation of the UserService */
class UserServiceImpl(clusterSharding: ClusterSharding, persistentEntityRegistry: PersistentEntityRegistry,
    readSide: ReadSide, processor: UserEventProcessor, database: UserDatabase)(implicit ec: ExecutionContext, auth: AuthenticationService) extends UserService {
  readSide.register(processor)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** Get all users from the database */
  override def getAllUsers(usernames: Option[String]): ServiceCall[NotUsed, GetAllUsersResponse] =
    authenticated[NotUsed, GetAllUsersResponse](AuthenticationRole.All: _*) {
      ServerServiceCall { (header, notUsed) =>
        for {
          students <- getAllStudents(usernames).invokeWithHeaders(header, notUsed)
          lecturers <- getAllLecturers(usernames).invokeWithHeaders(header, notUsed)
          admins <- getAllAdmins(usernames).invokeWithHeaders(header, notUsed)
        } yield (
          ResponseHeader(200, MessageProtocol.empty, List()),
          GetAllUsersResponse(students._2, lecturers._2, admins._2)
        )

      }
    }

  /** Delete a users from the database */
  override def deleteUser(username: String): ServiceCall[NotUsed, Done] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall { (_, _) =>
      val ref = entityRef(username)

      ref.ask[Confirmation](replyTo => DeleteUser(replyTo))
        .map {
          case Accepted => // Update Successful
            (ResponseHeader(200, MessageProtocol.empty, List()), Done)
          case Rejected("A user with the given username does not exist.") => // Already exists
            throw CustomException.NotFound
        }
    }
  }

  /** Get all students from the database */
  override def getAllStudents(usernames: Option[String]): ServerServiceCall[NotUsed, Seq[Student]] =
    identifiedAuthenticated[NotUsed, Seq[Student]](AuthenticationRole.All: _*) { (_, role) => _ =>
      usernames match {
        case None if role != AuthenticationRole.Admin =>
          throw CustomException.NotEnoughPrivileges
        case None if role == AuthenticationRole.Admin =>
          getAll(Role.Student).map(_.map(user => user.asInstanceOf[Student]))
        case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
          getAll(Role.Student).map(_.filter(student => listOfUsernames.split(',').contains(student.username)).map(user => user.asInstanceOf[Student].toPublic))
        case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
          getAll(Role.Student).map(_.filter(student => listOfUsernames.split(',').contains(student.username)).map(user => user.asInstanceOf[Student]))
      }
    }

  /** Update an existing student */
  override def updateStudent(username: String): ServiceCall[Student, Done] =
    ServerServiceCall { (header, user) =>
      updateUser(username).invokeWithHeaders(header, user)
    }

  /** Get all lecturers from the database */
  def getAllLecturers(usernames: Option[String]): ServerServiceCall[NotUsed, Seq[Lecturer]] =
    identifiedAuthenticated[NotUsed, Seq[Lecturer]](AuthenticationRole.All: _*) { (_, role) => _ =>
      usernames match {
        case None if role != AuthenticationRole.Admin =>
          throw CustomException.NotEnoughPrivileges
        case None if role == AuthenticationRole.Admin =>
          getAll(Role.Lecturer).map(_.map(user => user.asInstanceOf[Lecturer]))
        case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
          getAll(Role.Lecturer).map(_.filter(lecturer => listOfUsernames.split(',').contains(lecturer.username)).map(user => user.asInstanceOf[Lecturer].toPublic))
        case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
          getAll(Role.Lecturer).map(_.filter(lecturer => listOfUsernames.split(',').contains(lecturer.username)).map(user => user.asInstanceOf[Lecturer]))
      }
    }

  /** Update an existing lecturer */
  override def updateLecturer(username: String): ServiceCall[Lecturer, Done] =
    ServerServiceCall { (header, user) =>
      updateUser(username).invokeWithHeaders(header, user)
    }

  /** Get all admins from the database */
  override def getAllAdmins(usernames: Option[String]): ServerServiceCall[NotUsed, Seq[Admin]] =
    identifiedAuthenticated[NotUsed, Seq[Admin]](AuthenticationRole.All: _*) { (_, role) => _ =>
      usernames match {
        case None if role != AuthenticationRole.Admin =>
          throw CustomException.NotEnoughPrivileges
        case None if role == AuthenticationRole.Admin =>
          getAll(Role.Admin).map(_.map(user => user.asInstanceOf[Admin]))
        case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
          getAll(Role.Admin).map(_.filter(admin => listOfUsernames.split(',').contains(admin.username)).map(user => user.asInstanceOf[Admin].toPublic))
        case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
          getAll(Role.Admin).map(_.filter(admin => listOfUsernames.split(',').contains(admin.username)).map(user => user.asInstanceOf[Admin]))
      }
    }

  /** Update an existing admin */
  override def updateAdmin(username: String): ServiceCall[Admin, Done] =
    ServerServiceCall { (header, user) =>
      updateUser(username).invokeWithHeaders(header, user)
    }

  /** Get role of the user */
  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] =
    authenticated[NotUsed, JsonRole](AuthenticationRole.All: _*) {
      ServerServiceCall { (header, _) =>
        getUser(username).invokeWithHeaders(header, NotUsed).map {
          case (_, user) => (ResponseHeader(200, MessageProtocol.empty, List()), JsonRole(user.role))
        }
      }
    }

  /** Allows GET, PUT */
  override def allowedGetPut: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, PUT")

  /** Allows GET, POST */
  override def allowedGetPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, POST")

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows DELETE */
  override def allowedDelete: ServiceCall[NotUsed, Done] = allowedMethodsCustom("DELETE")

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Helper method for updating a generic User, independent of the role */
  private def updateUser(username: String): ServerServiceCall[User, Done] =
    identifiedAuthenticated(AuthenticationRole.All: _*) {
      (authUsername, role) =>
        ServerServiceCall { (header, user) =>
          // Check, if the username in path is different than the username in the object
          if (username != user.username.trim) {
            throw CustomException.PathParameterMismatch
          }
          // If invoked by a non-Admin, check if the manipulated object is owned by the user
          if (role != AuthenticationRole.Admin && authUsername != user.username.trim) {
            throw CustomException.OwnerMismatch
          }

          // We need to know what role the user has, because their editable fields are different
          getUser(username).invokeWithHeaders(header, NotUsed).map {
            case (_, oldUser) =>
              var err = oldUser.checkUneditableFields(user)
              if (role != AuthenticationRole.Admin) {
                err ++= oldUser.checkProtectedFields(user)
              }
              err
          }
            .flatMap { editErrors =>
              // Other users than admins can only edit specified fields
              if (editErrors.nonEmpty) {
                throw new CustomException(422, DetailedError("uneditable fields", editErrors))
              }
              else {
                val ref = entityRef(user.username)

                ref.ask[Confirmation](replyTo => UpdateUser(user, replyTo))
                  .map {
                    case Accepted => // Update successful
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case RejectedWithError(code, errorResponse) => //Update failed
                      throw new CustomException(code, errorResponse)
                  }
              }
            }
        }
    }

  /** Helper method for getting all Users */
  private def getAll(role: Role): Future[Seq[User]] = {
    database.getAll(role)
      .map(seq => seq
        .map(entityRef(_).ask[Option[User]](replyTo => GetUser(replyTo))) //Future[Seq[Future[Option[User]]]]
      )
      .flatMap(seq => Future.sequence(seq) //Future[Seq[Option[User]]]
        .map(seq => seq
          .filter(opt => opt.isDefined) //Get only existing users
          .map(opt => opt.get) //Future[Seq[User]]
        ))
  }

  /** Publishes every deletion of a user */
  def userDeletedTopic(): Topic[JsonUsername] = TopicProducer.singleStreamWithOffset { fromOffset =>
    persistentEntityRegistry
      .eventStream(UserEvent.Tag, fromOffset)
      .mapConcat {
        //Filter only OnUserDelete events
        case EventStreamElement(_, OnUserDelete(user), offset) =>
          immutable.Seq((JsonUsername(user.username), offset))
        case _ => Nil
      }
  }

  /** Update latestMatriculation */
  override def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done] = ServiceCall { matriculationUpdate =>
    val ref = entityRef(matriculationUpdate.username)
    ref.ask[Confirmation](replyTo => UpdateLatestMatriculation(matriculationUpdate.semester, replyTo)).map {
      case Accepted => Done
      case RejectedWithError(error, reason) => throw new CustomException(error, reason)
    }
  }

  /** Get a specific admin */
  override def getUser(username: String): ServerServiceCall[NotUsed, User] =
    identifiedAuthenticated(AuthenticationRole.All: _*) {
      (authUsername, role) =>
        ServerServiceCall { _ =>
          val ref = entityRef(username)

          ref.ask[Option[User]](replyTo => GetUser(replyTo)).map {
            case Some(user) =>
              if (role != AuthenticationRole.Admin && username != authUsername) {
                user.toPublic
              }
              else {
                user
              }
            case None => throw CustomException.NotFound
          }
        }
    }

  /** Adds the contents of the postMessageUser, authUser to authentication users and user to users */
  override def addUser: ServerServiceCall[PostMessageUser, User] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall { (_, postMessageUserRaw) =>

      val postMessageUser = postMessageUserRaw.clean

      val userTypeString = postMessageUser match {
        case _: PostMessageStudent  => "student"
        case _: PostMessageLecturer => "lecturer"
        case _: PostMessageAdmin    => "admin"
      }

      if (postMessageUser.authUser.username != postMessageUser.getUser.username) {
        throw new CustomException(
          422,
          DetailedError("validation error", Seq(
            SimpleError("authUser.username", "Username in authUser must match username in user"),
            SimpleError(userTypeString + ".username", "Username in user must match username in authUser")
          ))
        )
      }

      if (postMessageUser.getUser.username.isEmpty) {
        throw new CustomException(
          422,
          DetailedError("validation error", Seq(
            SimpleError("authUser.username", "Username must not be empty"),
            SimpleError(userTypeString + ".username", "Username must not be empty")
          ))
        )
      }

      val ref = entityRef(postMessageUser.getUser.username)

      ref.ask[Confirmation](replyTo => CreateUser(postMessageUser.getUser, replyTo))
        .flatMap {
          case Accepted => // Creation Successful
            auth.setAuthentication().invoke(postMessageUser.authUser)
              .map { _ =>
                val header = ResponseHeader(201, MessageProtocol.empty, List())
                  .addHeader("Location", s"$pathPrefix/users/students/${postMessageUser.getUser.username}")
                (header, postMessageUser.getUser)
              }
              //In case the password cant be saved
              .recoverWith {
                case authenticationException: CustomException =>
                  ref.ask[Confirmation](replyTo => DeleteUser(replyTo))
                    .map[(ResponseHeader, User)] { _ =>
                      //the deletion of the user was successful after the error in the authentication service
                      if (authenticationException.getPossibleErrorResponse.`type` == "validation error") {
                        val detailedError = authenticationException.getPossibleErrorResponse.asInstanceOf[DetailedError]
                        throw new CustomException(
                          authenticationException.getErrorCode,
                          detailedError.copy(invalidParams = detailedError
                            .invalidParams.map(error => error.copy(name = "authUser. " + error.name)))
                        )
                      }
                      else {
                        throw authenticationException
                      }
                    }
                    .recover {
                      case deletionException: Exception => throw deletionException //the deletion didnt work, a ghost user does now exist
                    }
              }
          case RejectedWithError(code, errorResponse) =>
            throw new CustomException(code, errorResponse)
        }
    }
  }
}