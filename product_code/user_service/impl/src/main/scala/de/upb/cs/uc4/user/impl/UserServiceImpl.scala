package de.upb.cs.uc4.user.impl

import java.util

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.{ ByteString, Timeout }
import akka.{ Done, NotUsed }
import com.google.common.io.ByteStreams
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{ EventStreamElement, PersistentEntityRegistry, ReadSide }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, JsonUsername }
import de.upb.cs.uc4.image.api.ImageProcessingService
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.server.Hashing
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected, RejectedWithError }
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.UserState
import de.upb.cs.uc4.user.impl.commands._
import de.upb.cs.uc4.user.impl.events.{ OnUserCreate, OnUserDelete, UserEvent }
import de.upb.cs.uc4.user.impl.readside.{ UserDatabase, UserEventProcessor }
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model._
import de.upb.cs.uc4.user.model.post.{ PostMessageAdmin, PostMessageLecturer, PostMessageStudent, PostMessageUser }
import de.upb.cs.uc4.user.model.user._

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/** Implementation of the UserService */
class UserServiceImpl(
    clusterSharding: ClusterSharding, persistentEntityRegistry: PersistentEntityRegistry,
    readSide: ReadSide, processor: UserEventProcessor, database: UserDatabase,
    authentication: AuthenticationService, imageProcessing: ImageProcessingService
)(implicit ec: ExecutionContext, config: Config) extends UserService {
  readSide.register(processor)

  private lazy val defaultProfilePicture = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultProfile.png"))
  private lazy val defaultThumbnail = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultThumbnail.png"))
  private lazy val supportedTypes: util.List[String] = config.getStringList("uc4.image.supportedTypes")
  private lazy val convertedTypes: util.List[String] = config.getStringList("uc4.image.convertedTypes")
  private lazy val profileWidth: Int = config.getInt("uc4.image.profileWidth")
  private lazy val profileHeight: Int = config.getInt("uc4.image.profileHeight")
  private lazy val thumbnailWidth: Int = config.getInt("uc4.image.thumbnailWidth")
  private lazy val thumbnailHeight: Int = config.getInt("uc4.image.thumbnailHeight")

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.validation.timeout").milliseconds

  /** Get the specified user */
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
            case None => throw UC4Exception.NotFound
          }
        }
    }

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

  /** Adds the contents of the postMessageUser, authUser to authentication users and user to users */
  override def addUser(): ServerServiceCall[PostMessageUser, User] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall { (_, postMessageUserRaw) =>
      val postMessageUser = postMessageUserRaw.clean

      val userVariableName = postMessageUser match {
        case _: PostMessageStudent  => "student"
        case _: PostMessageLecturer => "lecturer"
        case _: PostMessageAdmin    => "admin"
      }

      val validationErrorsFuture = postMessageUser match {
        case postMessageStudent: PostMessageStudent =>
          //For students we may encounter duplicate matriculationIDs
          postMessageStudent.validate.flatMap { studentValidationErrorsImmutable =>

            var studentValidationErrors = studentValidationErrorsImmutable
            val student = postMessageUser.getUser.asInstanceOf[Student]
            getAll(Role.Student).map(_.map(_.asInstanceOf[Student].matriculationId).contains(student.matriculationId)).map {
              matDuplicate =>
                if (matDuplicate) {
                  studentValidationErrors :+= SimpleError("student.matriculationId", "MatriculationID already in use.")
                }
                studentValidationErrors
            }
          }

        case _ =>
          //For other users we cannot encounter duplicate matriculationIDs
          postMessageUser.validate
      }

      var validationErrors = try {
        Await.result(validationErrorsFuture, validationTimeout)
      }
      catch {
        case _: TimeoutException => throw UC4Exception.ValidationTimeout
        case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
      }

      // Check, if username errors exist, since entityRef might fail if username is incorrect
      if (validationErrors.map(_.name).contains(userVariableName + ".username")) {
        throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
      }

      val ref = entityRef(postMessageUser.getUser.username)
      ref.ask[Option[User]](replyTo => GetUser(replyTo)).flatMap { optUser =>
        // If username is already in use, add that error to the validation list
        if (optUser.isDefined) {
          validationErrors :+= SimpleError(userVariableName + ".username", "Username already in use.")
        }

        if (validationErrors.nonEmpty) {
          throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
        }

        ref.ask[Confirmation](replyTo => CreateUser(postMessageUser.getUser, replyTo))
          .flatMap {
            case Accepted => // Creation Successful
              authentication.setAuthentication().invoke(postMessageUser.authUser)
                .map { _ =>
                  val header = ResponseHeader(201, MessageProtocol.empty, List())
                    .addHeader("Location", s"$pathPrefix/users/students/${postMessageUser.getUser.username}")
                  (header, postMessageUser.getUser)
                }
                // In case the password cant be saved
                .recoverWith {
                  case authenticationException: UC4Exception =>
                    ref.ask[Confirmation](replyTo => DeleteUser(replyTo))
                      .map[(ResponseHeader, User)] { _ =>
                        throw authenticationException
                      }
                      .recover {
                        case deletionException: Exception =>
                          //the deletion didn't work, a ghost user does now exist
                          throw deletionException
                      }
                }
            case RejectedWithError(code, errorResponse) =>
              throw new UC4Exception(code, errorResponse)
          }
      }
    }
  }

  /** Helper method for updating a generic User, independent of the role */
  override def updateUser(username: String): ServerServiceCall[User, Done] =
    identifiedAuthenticated(AuthenticationRole.All: _*) {
      (authUsername, role) =>
        ServerServiceCall {
          (_, userRaw) =>
            val user = userRaw.clean
            // Check, if the username in path is different than the username in the object
            if (username != user.username) {
              throw UC4Exception.PathParameterMismatch
            }

            // If invoked by a non-Admin, check if the manipulated object is owned by the user
            if (role != AuthenticationRole.Admin && authUsername != user.username) {
              throw UC4Exception.OwnerMismatch
            }

            //validate new user and check, if username errors exist, since entityRef might fail if username is incorrect
            var validationErrors = try {
              Await.result(user.validate, validationTimeout)
            }
            catch {
              case _: TimeoutException => throw UC4Exception.ValidationTimeout
              case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
            }

            if (validationErrors.map(_.name).contains("username")) {
              throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
            }

            val ref = entityRef(user.username)
            ref.ask[Option[User]](replyTo => GetUser(replyTo)).flatMap {
              optUser =>
                if (optUser.isEmpty) {
                  // Add to validation errors, and throw prematurely since uneditable fields are uncheckable
                  validationErrors :+= SimpleError("username", "Username not in use.")
                  throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
                }
                val oldUser = optUser.get
                // Other users than admins can only edit specified fields
                var editErrors = oldUser.checkUneditableFields(user)
                if (role != AuthenticationRole.Admin) {
                  editErrors ++= oldUser.checkProtectedFields(user)
                }
                if (editErrors.nonEmpty) {
                  throw new UC4Exception(422, DetailedError(ErrorType.UneditableFields, editErrors))
                }
                if (validationErrors.nonEmpty) {
                  throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
                }

                oldUser match {
                  case _: Student if !user.isInstanceOf[Student] => throw new UC4Exception(400, InformativeError(ErrorType.UnexpectedEntity, "Expected Student, but received non-Student"))
                  case _: Lecturer if !user.isInstanceOf[Lecturer] => throw new UC4Exception(400, InformativeError(ErrorType.UnexpectedEntity, "Expected Lecturer, but received non-Lecturer"))
                  case _: Admin if !user.isInstanceOf[Admin] => throw new UC4Exception(400, InformativeError(ErrorType.UnexpectedEntity, "Expected Admin, but received non-Admin"))
                  case _ =>
                }

                ref.ask[Confirmation](replyTo => UpdateUser(user, replyTo))
                  .map {
                    case Accepted => // Update successful
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case RejectedWithError(code, errorResponse) => //Update failed
                      throw new UC4Exception(code, errorResponse)
                  }
            }
        }
    }

  /** Delete a users from the database */
  override def deleteUser(username: String): ServiceCall[NotUsed, Done] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall {
      (_, _) =>
        val ref = entityRef(username)

        ref.ask[Confirmation](replyTo => DeleteUser(replyTo))
          .map {
            case Accepted => // Update Successful
              (ResponseHeader(200, MessageProtocol.empty, List()), Done)
            case Rejected("A user with the given username does not exist.") => // Already exists
              throw UC4Exception.NotFound
          }
    }
  }

  /** Get all students from the database */
  override def getAllStudents(usernames: Option[String]): ServerServiceCall[NotUsed, Seq[Student]] =
    identifiedAuthenticated[NotUsed, Seq[Student]](AuthenticationRole.All: _*) { (_, role) => _ =>
      usernames match {
        case None if role != AuthenticationRole.Admin =>
          throw UC4Exception.NotEnoughPrivileges
        case None if role == AuthenticationRole.Admin =>
          getAll(Role.Student).map(_.map(user => user.asInstanceOf[Student]))
        case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
          getAll(Role.Student).map(_.filter(student => listOfUsernames.split(',').contains(student.username)).map(user => user.asInstanceOf[Student].toPublic))
        case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
          getAll(Role.Student).map(_.filter(student => listOfUsernames.split(',').contains(student.username)).map(user => user.asInstanceOf[Student]))
      }
    }

  /** Get all lecturers from the database */
  def getAllLecturers(usernames: Option[String]): ServerServiceCall[NotUsed, Seq[Lecturer]] =
    identifiedAuthenticated[NotUsed, Seq[Lecturer]](AuthenticationRole.All: _*) { (_, role) => _ =>
      usernames match {
        case None if role != AuthenticationRole.Admin =>
          throw UC4Exception.NotEnoughPrivileges
        case None if role == AuthenticationRole.Admin =>
          getAll(Role.Lecturer).map(_.map(user => user.asInstanceOf[Lecturer]))
        case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
          getAll(Role.Lecturer).map(_.filter(lecturer => listOfUsernames.split(',').contains(lecturer.username)).map(user => user.asInstanceOf[Lecturer].toPublic))
        case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
          getAll(Role.Lecturer).map(_.filter(lecturer => listOfUsernames.split(',').contains(lecturer.username)).map(user => user.asInstanceOf[Lecturer]))
      }
    }

  /** Get all admins from the database */
  override def getAllAdmins(usernames: Option[String]): ServerServiceCall[NotUsed, Seq[Admin]] =
    identifiedAuthenticated[NotUsed, Seq[Admin]](AuthenticationRole.All: _*) { (_, role) => _ =>
      usernames match {
        case None if role != AuthenticationRole.Admin =>
          throw UC4Exception.NotEnoughPrivileges
        case None if role == AuthenticationRole.Admin =>
          getAll(Role.Admin).map(_.map(user => user.asInstanceOf[Admin]))
        case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
          getAll(Role.Admin).map(_.filter(admin => listOfUsernames.split(',').contains(admin.username)).map(user => user.asInstanceOf[Admin].toPublic))
        case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
          getAll(Role.Admin).map(_.filter(admin => listOfUsernames.split(',').contains(admin.username)).map(user => user.asInstanceOf[Admin]))
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

  /** Get role of the user */
  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] =
    authenticated[NotUsed, JsonRole](AuthenticationRole.All: _*) {
      ServerServiceCall {
        (header, _) =>
          getUser(username).invokeWithHeaders(header, NotUsed).map {
            case (_, user) => (ResponseHeader(200, MessageProtocol.empty, List()), JsonRole(user.role))
          }
      }
    }

  /** Update latestMatriculation */
  override def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done] = ServiceCall {
    matriculationUpdate =>
      val ref = entityRef(matriculationUpdate.username)
      ref.ask[Confirmation](replyTo => UpdateLatestMatriculation(matriculationUpdate.semester, replyTo)).map {
        case Accepted => Done
        case RejectedWithError(error, reason) => throw new UC4Exception(error, reason)
      }
  }

  /** Publishes every new user */
  override def userCreationTopic(): Topic[Usernames] = TopicProducer.singleStreamWithOffset { fromOffset =>
    persistentEntityRegistry
      .eventStream(UserEvent.Tag, fromOffset)
      .mapConcat {
        //Filter only OnUserCreate events
        case EventStreamElement(_, OnUserCreate(user), offset) =>
          immutable.Seq((Usernames(user.username, Hashing.sha256(user.username)), offset))
        case _ => Nil
      }
  }

  /** Publishes every deletion of a user */
  override def userDeletedTopic(): Topic[JsonUsername] = TopicProducer.singleStreamWithOffset {
    fromOffset =>
      persistentEntityRegistry
        .eventStream(UserEvent.Tag, fromOffset)
        .mapConcat {
          //Filter only OnUserDelete events
          case EventStreamElement(_, OnUserDelete(user), offset) =>
            immutable.Seq((JsonUsername(user.username), offset))
          case _ => Nil
        }
  }

  /** Gets the image of the user */
  override def getImage(username: String): ServiceCall[NotUsed, ByteString] = authenticated[NotUsed, ByteString](AuthenticationRole.All: _*) {
    ServerServiceCall {
      (header, _) =>
        database.getImage(username).flatMap {
          case Some((array, contentType)) =>
            Future.successful(ResponseHeader(200, MessageProtocol(contentType = Some(s"$contentType; charset=UTF-8")), List()), ByteString(array))
          case None =>
            getUser(username).invokeWithHeaders(header, NotUsed).map {
              _ =>
                (ResponseHeader(200, MessageProtocol(contentType = Some(s"image/png; charset=UTF-8")), List()), ByteString(defaultProfilePicture))
            }
        }
    }
  }

  /** Gets the thumbnail of the user */
  override def getThumbnail(username: String): ServiceCall[NotUsed, ByteString] = authenticated[NotUsed, ByteString](AuthenticationRole.All: _*) {
    ServerServiceCall {
      (header, _) =>
        database.getThumbnail(username).flatMap {
          case Some((array, contentType)) =>
            Future.successful(ResponseHeader(200, MessageProtocol(contentType = Some(s"$contentType; charset=UTF-8")), List()), ByteString(array))
          case None =>
            getUser(username).invokeWithHeaders(header, NotUsed).map {
              _ =>
                (ResponseHeader(200, MessageProtocol(contentType = Some(s"image/png; charset=UTF-8")), List()), ByteString(defaultThumbnail))
            }
        }
    }
  }

  /** Sets the image of the user */
  override def setImage(username: String): ServerServiceCall[Array[Byte], Done] =
    identifiedAuthenticated[Array[Byte], Done](AuthenticationRole.All: _*) {
      (authUsername, role) =>
        ServerServiceCall {
          (header, image) =>
            if (role != AuthenticationRole.Admin && authUsername != username) {
              throw UC4Exception.OwnerMismatch
            }

            header.getHeader("Content-Type") match {
              case Some(contentTypeRaw) =>
                var contentType = contentTypeRaw.trim
                if (supportedTypes.contains(contentType)) {
                  getUser(username).invokeWithHeaders(header, NotUsed).flatMap {
                    _ =>
                      val convertedFuture = if (convertedTypes.contains(contentType)) {
                        contentType = "image/jpeg"
                        imageProcessing.convert("jpeg").invoke(ByteString(image))
                      }
                      else {
                        Future.successful(ByteString(image))
                      }

                      convertedFuture.flatMap { converted =>
                        imageProcessing.fit(profileWidth, profileHeight).invoke(converted).flatMap { profilePicture =>
                          imageProcessing.thumbnail(thumbnailWidth, thumbnailHeight).invoke(profilePicture).flatMap { thumbnail =>
                            database.setImage(username, profilePicture.toArray, thumbnail.toArray, contentType).map {
                              _ =>
                                (ResponseHeader(200, MessageProtocol.empty, List(("Location", s"$pathPrefix/users/$username/image"))), Done)
                            }
                          }
                        }
                      }
                        .recover {
                          case ex: Throwable => throw UC4Exception.InternalServerError("Something gone wrong", ex.getMessage)
                        }
                  }
                }
                else {
                  throw UC4Exception.UnsupportedMediaType
                }
              case None => throw new UC4Exception(400, DetailedError(ErrorType.MissingHeader, Seq(SimpleError("Content-Type", "Missing"))))
            }
        }
    }

  /** Delete the image of the user */
  def deleteImage(username: String): ServiceCall[NotUsed, Done] = authenticated[NotUsed, Done](AuthenticationRole.All: _*) {
    ServerServiceCall {
      (header, _) =>
        getUser(username).invokeWithHeaders(header, NotUsed).flatMap {
          _ =>
            database.deleteImage(username).map {
              _ =>
                (ResponseHeader(200, MessageProtocol.empty, List()), Done)
            }
        }
    }
  }

  /** Allows GET, POST */
  override def allowedGetPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, POST")

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows DELETE, GET, PUT */
  override def allowedDeleteGetPut: ServiceCall[NotUsed, Done] = allowedMethodsCustom("DELETE, GET, PUT")

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
