package de.upb.cs.uc4.user.impl

import java.util

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.{ ByteString, Timeout }
import akka.{ Done, NotUsed }
import com.google.common.io.{ BaseEncoding, ByteStreams }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{ EventStreamElement, PersistentEntityRegistry }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, JsonUsername }
import de.upb.cs.uc4.image.api.ImageProcessingService
import de.upb.cs.uc4.shared.client.Hashing
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionUtility
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.UserState
import de.upb.cs.uc4.user.impl.commands._
import de.upb.cs.uc4.user.impl.events.{ OnUserCreate, OnUserForceDelete, OnUserSoftDelete, UserEvent }
import de.upb.cs.uc4.user.impl.readside.UserDatabase
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.user._
import de.upb.cs.uc4.user.model.{ PostMessageUser, _ }
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }
import scala.util.Random

/** Implementation of the UserService */
class UserServiceImpl(
    clusterSharding: ClusterSharding, persistentEntityRegistry: PersistentEntityRegistry, database: UserDatabase,
    authentication: AuthenticationService, kafkaEncryptionUtility: KafkaEncryptionUtility, imageProcessing: ImageProcessingService,
    override val environment: Environment
)(implicit ec: ExecutionContext, timeout: Timeout, config: Config) extends UserService {

  private final val log: Logger = LoggerFactory.getLogger(classOf[UserServiceImpl])

  private lazy val defaultProfilePicture = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultProfile.jpg"))
  private lazy val defaultThumbnail = ByteStreams.toByteArray(getClass.getResourceAsStream("/DefaultThumbnail.jpg"))
  private lazy val supportedTypes: util.List[String] = config.getStringList("uc4.image.supportedTypes")
  private lazy val profileWidth: Int = config.getInt("uc4.image.profileWidth")
  private lazy val profileHeight: Int = config.getInt("uc4.image.profileHeight")
  private lazy val thumbnailWidth: Int = config.getInt("uc4.image.thumbnailWidth")
  private lazy val thumbnailHeight: Int = config.getInt("uc4.image.thumbnailHeight")

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserState.typeKey, id)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds
  lazy val internalQueryTimeout: FiniteDuration = config.getInt("uc4.timeouts.database").milliseconds

  /** Get the specified user */
  override def getUser(username: String): ServerServiceCall[NotUsed, User] =
    identifiedAuthenticated(AuthenticationRole.All: _*) {
      (authUsername, role) =>
        ServerServiceCall { (header, _) =>
          val ref = entityRef(username)

          ref.ask[Option[User]](replyTo => GetUser(replyTo)).map {
            case Some(user) =>
              if (role != AuthenticationRole.Admin && username != authUsername) {
                createETagHeader(header, user.toPublic)
              }
              else {
                createETagHeader(header, user)
              }
            case None => throw UC4Exception.NotFound
          }
        }
    }

  /** Get all users from the database */
  override def getAllUsers(usernames: Option[String], isActive: Option[Boolean]): ServiceCall[NotUsed, GetAllUsersResponse] =
    authenticated[NotUsed, GetAllUsersResponse](AuthenticationRole.All: _*) {
      ServerServiceCall { (header, notUsed) =>
        for {
          students <- getAllStudents(usernames, isActive).invokeWithHeaders(header, notUsed)
          lecturers <- getAllLecturers(usernames, isActive).invokeWithHeaders(header, notUsed)
          admins <- getAllAdmins(usernames, isActive).invokeWithHeaders(header, notUsed)
        } yield createETagHeader(header, GetAllUsersResponse(students._2, lecturers._2, admins._2))
      }
    }

  /** Adds the contents of the postMessageUser, authUser to authentication users and user to users */
  override def addUser(): ServerServiceCall[PostMessageUser, User] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall { (_, postMessageUserRaw) =>
      val postMessageUser = postMessageUserRaw.clean

      var PostMessageUserErrors = try {
        Await.result(postMessageUser.validate, validationTimeout)
      }
      catch {
        case _: TimeoutException => throw UC4Exception.ValidationTimeout
        case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
      }

      val validationErrorsFuture = postMessageUser.user match {
        case student: Student =>
          //For students we may encounter duplicate matriculationIDs
          getAll(Role.Student).map(_.map(_.asInstanceOf[Student].matriculationId).contains(student.matriculationId)).map {
            matDuplicate =>
              if (matDuplicate) {
                PostMessageUserErrors :+= SimpleError("user.matriculationId", "MatriculationID already in use.")
              }
              PostMessageUserErrors
          }

        case _ =>
          //For other users we cannot encounter duplicate matriculationIDs
          Future.successful(PostMessageUserErrors)
      }

      var validationErrors = try {
        Await.result(validationErrorsFuture, internalQueryTimeout)
      }
      catch {
        case _: TimeoutException => throw UC4Exception.ValidationTimeout
        case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
      }

      // Check, if username errors exist, since entityRef might fail if username is incorrect
      if (validationErrors.map(_.name).contains("user.username")) {
        throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
      }

      val ref = entityRef(postMessageUser.user.username)
      ref.ask[Option[User]](replyTo => GetUser(replyTo)).flatMap { optUser =>
        // If username is already in use, add that error to the validation list
        if (optUser.isDefined) {
          validationErrors :+= SimpleError("user.username", "Username already in use.")
        }

        if (validationErrors.nonEmpty) {
          throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
        }

        //Set enrollmentIdSecret in User object
        val postMessageFinalized = postMessageUser.copy(user =
          postMessageUser.user.copyUser(enrollmentIdSecret = createEnrollmentIdSecret()))

        ref.ask[Confirmation](replyTo => CreateUser(postMessageFinalized.user, postMessageFinalized.governmentId, replyTo))
          .flatMap {
            case Accepted(_) => // Creation Successful
              authentication.setAuthentication().invoke(postMessageFinalized.authUser)
                .map { _ =>
                  val header = ResponseHeader(201, MessageProtocol.empty, List())
                    .addHeader("Location", s"$pathPrefix/users/students/${postMessageFinalized.user.username}")
                  (header, postMessageFinalized.user)
                }
                // In case the password cant be saved
                .recoverWith {
                  case authenticationException: UC4Exception =>
                    ref.ask[Confirmation](replyTo => ForceDeleteUser(replyTo))
                      .map[(ResponseHeader, User)] { _ =>
                        throw authenticationException
                      }
                      .recover {
                        case deletionException: Exception =>
                          //the deletion didn't work, a ghost user does now exist
                          throw deletionException
                      }
                }
            case Rejected(code, reason) =>
              throw UC4Exception(code, reason)
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
              throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
            }

            val ref = entityRef(user.username)
            ref.ask[Option[User]](replyTo => GetUser(replyTo)).flatMap {
              optUser =>
                if (optUser.isEmpty) {
                  // Add to validation errors, and throw prematurely since uneditable fields are uncheckable
                  validationErrors :+= SimpleError("username", "Username not in use.")
                  throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
                }
                val oldUser = optUser.get
                // Other users than admins can only edit specified fields
                var editErrors = oldUser.checkUneditableFields(user)
                if (role != AuthenticationRole.Admin) {
                  editErrors ++= oldUser.checkProtectedFields(user)
                }
                if (editErrors.nonEmpty) {
                  throw new UC4NonCriticalException(422, DetailedError(ErrorType.UneditableFields, editErrors))
                }
                if (validationErrors.nonEmpty) {
                  throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
                }

                oldUser match {
                  case _: Student if !user.isInstanceOf[Student] => throw new UC4NonCriticalException(400, InformativeError(ErrorType.UnexpectedEntity, "Expected Student, but received non-Student"))
                  case _: Lecturer if !user.isInstanceOf[Lecturer] => throw new UC4NonCriticalException(400, InformativeError(ErrorType.UnexpectedEntity, "Expected Lecturer, but received non-Lecturer"))
                  case _: Admin if !user.isInstanceOf[Admin] => throw new UC4NonCriticalException(400, InformativeError(ErrorType.UnexpectedEntity, "Expected Admin, but received non-Admin"))
                  case _ =>
                }

                ref.ask[Confirmation](replyTo => UpdateUser(user, replyTo))
                  .map {
                    case Accepted(_) => // Update successful
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case Rejected(code, reason) => //Update failed
                      throw UC4Exception(code, reason)
                  }
            }
        }
    }

  /** Completely deletes a users from the database */
  override def forceDeleteUser(username: String): ServiceCall[NotUsed, Done] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall {
      (_, _) =>
        val ref = entityRef(username)
        ref.ask[Confirmation](replyTo => ForceDeleteUser(replyTo))
          .map {
            case Accepted(_) => // Update Successful
              (ResponseHeader(200, MessageProtocol.empty, List()), Done)
            case Rejected(code, reason) =>
              throw UC4Exception(code, reason)
          }
    }
  }

  /** Flags a user as deleted and deletes personal info from now on unrequired */
  override def softDeleteUser(username: String): ServiceCall[NotUsed, Done] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall {
      (_, _) =>
        val ref = entityRef(username)
        ref.ask[Option[User]](replyTo => GetUser(replyTo)).flatMap {
          optUser =>
            if (optUser.isEmpty) {
              throw UC4Exception.NotFound
            }

            if (!optUser.get.isActive) {
              throw UC4Exception.AlreadyDeleted
            }

            ref.ask[Confirmation](replyTo => SoftDeleteUser(replyTo))
              .map {
                case Accepted(_) => // Soft Deletion successful
                  (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                case Rejected(code, reason) => //Update failed
                  throw UC4Exception(code, reason)
              }
        }
    }
  }

  /** Get all students from the database */
  override def getAllStudents(usernames: Option[String], isActive: Option[Boolean]): ServerServiceCall[NotUsed, Seq[Student]] =
    identifiedAuthenticated[NotUsed, Seq[Student]](AuthenticationRole.All: _*) { (_, role) =>
      ServerServiceCall { (header, _) =>
        (usernames match {
          case None if role != AuthenticationRole.Admin =>
            throw UC4Exception.NotEnoughPrivileges
          case None if role == AuthenticationRole.Admin =>
            getAll(Role.Student).map(_.map(user => user.asInstanceOf[Student]))
          case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
            getAll(Role.Student).map(_.filter(student => listOfUsernames.split(',').contains(student.username)).map(user => user.asInstanceOf[Student].toPublic))
          case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
            getAll(Role.Student).map(_.filter(student => listOfUsernames.split(',').contains(student.username)).map(user => user.asInstanceOf[Student]))
        }).map { students =>
          createETagHeader(header, students.filter(isActive.isEmpty || _.isActive == isActive.get))
        }
      }
    }

  /** Get all lecturers from the database */
  override def getAllLecturers(usernames: Option[String], isActive: Option[Boolean]): ServerServiceCall[NotUsed, Seq[Lecturer]] =
    identifiedAuthenticated[NotUsed, Seq[Lecturer]](AuthenticationRole.All: _*) { (_, role) =>
      ServerServiceCall { (header, _) =>
        (usernames match {
          case None if role != AuthenticationRole.Admin =>
            throw UC4Exception.NotEnoughPrivileges
          case None if role == AuthenticationRole.Admin =>
            getAll(Role.Lecturer).map(_.map(user => user.asInstanceOf[Lecturer]))
          case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
            getAll(Role.Lecturer).map(_.filter(lecturer => listOfUsernames.split(',').contains(lecturer.username)).map(user => user.asInstanceOf[Lecturer].toPublic))
          case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
            getAll(Role.Lecturer).map(_.filter(lecturer => listOfUsernames.split(',').contains(lecturer.username)).map(user => user.asInstanceOf[Lecturer]))
        }).map { lecturers =>
          createETagHeader(header, lecturers.filter(isActive.isEmpty || _.isActive == isActive.get))
        }
      }
    }

  /** Get all admins from the database */
  override def getAllAdmins(usernames: Option[String], isActive: Option[Boolean]): ServerServiceCall[NotUsed, Seq[Admin]] =
    identifiedAuthenticated[NotUsed, Seq[Admin]](AuthenticationRole.All: _*) { (_, role) =>
      ServerServiceCall { (header, _) =>
        (usernames match {
          case None if role != AuthenticationRole.Admin =>
            throw UC4Exception.NotEnoughPrivileges
          case None if role == AuthenticationRole.Admin =>
            getAll(Role.Admin).map(_.map(user => user.asInstanceOf[Admin]))
          case Some(listOfUsernames) if role != AuthenticationRole.Admin =>
            getAll(Role.Admin).map(_.filter(admin => listOfUsernames.split(',').contains(admin.username)).map(user => user.asInstanceOf[Admin].toPublic))
          case Some(listOfUsernames) if role == AuthenticationRole.Admin =>
            getAll(Role.Admin).map(_.filter(admin => listOfUsernames.split(',').contains(admin.username)).map(user => user.asInstanceOf[Admin]))
        }).map { admins =>
          createETagHeader(header, admins.filter(isActive.isEmpty || _.isActive == isActive.get))
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

  /** Get role of the user */
  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] =
    authenticated[NotUsed, JsonRole](AuthenticationRole.All: _*) {
      ServerServiceCall {
        (header, _) =>
          getUser(username).invokeWithHeaders(header, NotUsed).map {
            case (_, user) => createETagHeader(header, JsonRole(user.role))
          }
      }
    }

  /** Update latestMatriculation */
  override def updateLatestMatriculation(): ServiceCall[MatriculationUpdate, Done] = ServiceCall {
    matriculationUpdate =>
      val ref = entityRef(matriculationUpdate.username)
      ref.ask[Confirmation](replyTo => UpdateLatestMatriculation(matriculationUpdate.semester, replyTo)).map {
        case Accepted(_)             => Done
        case Rejected(error, reason) => throw UC4Exception(error, reason)
      }
  }

  /** Publishes every new user */
  override def userCreationTopic(): Topic[EncryptionContainer] = TopicProducer.singleStreamWithOffset { fromOffset =>
    persistentEntityRegistry
      .eventStream(UserEvent.Tag, fromOffset)
      .mapConcat {
        //Filter only OnUserCreate events
        case EventStreamElement(_, OnUserCreate(user, governmentId), offset) => try {
          immutable.Seq((kafkaEncryptionUtility.encrypt(Usernames(user.username, Hashing.sha256(s"$governmentId${user.enrollmentIdSecret}"))), offset))
        }
        catch {
          case throwable: Throwable =>
            log.error("UserService cannot send invalid creation topic message {}", throwable.toString)
            Nil
        }
        case _ => Nil
      }
  }

  /** Publishes every deletion of a user */
  override def userDeletionTopicMinimal(): Topic[EncryptionContainer] = TopicProducer.singleStreamWithOffset {
    fromOffset =>
      persistentEntityRegistry
        .eventStream(UserEvent.Tag, fromOffset)
        .mapConcat {
          //Filter ForceDelete events
          case EventStreamElement(_, OnUserForceDelete(user), offset) => try {
            immutable.Seq((kafkaEncryptionUtility.encrypt(JsonUsername(user.username)), offset))
          }
          catch {
            case throwable: Throwable =>
              log.error("UserService cannot send invalid minimal force delete topic message {}", throwable.toString)
              Nil
          }
          //Filter SoftDeleteEvents
          case EventStreamElement(_, OnUserSoftDelete(user), offset) => try {
            immutable.Seq((kafkaEncryptionUtility.encrypt(JsonUsername(user.username)), offset))
          }
          catch {
            case throwable: Throwable =>
              log.error("UserService cannot send invalid minimal soft delete topic message {}", throwable.toString)
              Nil
          }
          case _ => Nil
        }
  }

  /** Publishes every deletion of a user */
  override def userDeletionTopicPrecise(): Topic[EncryptionContainer] = TopicProducer.singleStreamWithOffset {
    fromOffset =>
      persistentEntityRegistry
        .eventStream(UserEvent.Tag, fromOffset)
        .mapConcat {
          //Filter ForceDelete events
          case EventStreamElement(_, OnUserForceDelete(user), offset) => try {
            immutable.Seq((kafkaEncryptionUtility.encrypt(JsonUserData(user.username, user.role, forceDelete = true)), offset))
          }
          catch {
            case throwable: Throwable =>
              log.error("UserService cannot send invalid precise force delete topic message {}", throwable.toString)
              Nil
          }
          //Filter SoftDelete events
          case EventStreamElement(_, OnUserSoftDelete(user), offset) => try {
            immutable.Seq((kafkaEncryptionUtility.encrypt(JsonUserData(user.username, user.role, forceDelete = false)), offset))
          }
          catch {
            case throwable: Throwable =>
              log.error("UserService cannot send invalid precise soft delete topic message {}", throwable.toString)
              Nil
          }
          case _ => Nil
        }
  }

  /** Gets the image of the user */
  override def getImage(username: String): ServiceCall[NotUsed, ByteString] = authenticated[NotUsed, ByteString](AuthenticationRole.All: _*) {
    ServerServiceCall {
      (header, _) =>
        database.getImage(username).flatMap {
          case Some(array) =>
            Future.successful(ResponseHeader(200, MessageProtocol(contentType = Some("image/jpeg; charset=UTF-8")), List())
              .addHeader("ETag", checkImageETag(header, array)), ByteString(array))
          case None =>
            getUser(username).invokeWithHeaders(header, NotUsed).map {
              _ =>
                (ResponseHeader(200, MessageProtocol(contentType = Some("image/png; charset=UTF-8")), List())
                  .addHeader("ETag", checkImageETag(header, defaultProfilePicture)), ByteString(defaultProfilePicture))
            }
        }
    }
  }

  /** Gets the thumbnail of the user */
  override def getThumbnail(username: String): ServiceCall[NotUsed, ByteString] = authenticated[NotUsed, ByteString](AuthenticationRole.All: _*) {
    ServerServiceCall {
      (header, _) =>
        database.getThumbnail(username).flatMap {
          case Some(array) =>
            Future.successful(ResponseHeader(200, MessageProtocol(contentType = Some("image/jpeg; charset=UTF-8")), List())
              .addHeader("ETag", checkImageETag(header, array)), ByteString(array))
          case None =>
            getUser(username).invokeWithHeaders(header, NotUsed).map {
              _ =>
                (ResponseHeader(200, MessageProtocol(contentType = Some("image/png; charset=UTF-8")), List())
                  .addHeader("ETag", checkImageETag(header, defaultThumbnail)), ByteString(defaultThumbnail))
            }
        }
    }
  }

  protected def checkImageETag(serviceHeader: RequestHeader, image: Array[Byte]): String = {
    val eTag = serviceHeader.getHeader("If-None-Match").getOrElse("")
    val newTag = Hashing.sha256(image)

    if (newTag == eTag) {
      throw UC4Exception.NotModified
    }
    else {
      newTag
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
                val contentType = contentTypeRaw.trim
                if (supportedTypes.contains(contentType)) {
                  getUser(username).invokeWithHeaders(header, NotUsed).flatMap {
                    _ =>
                      imageProcessing.convert("jpeg").invoke(ByteString(image)).flatMap { converted =>
                        imageProcessing.smartCrop(profileWidth, profileHeight).invoke(converted).flatMap { profilePicture =>
                          imageProcessing.thumbnail(thumbnailWidth, thumbnailHeight).invoke(profilePicture).flatMap { thumbnail =>
                            database.setImage(username, profilePicture.toArray, thumbnail.toArray).map {
                              _ =>
                                (ResponseHeader(200, MessageProtocol.empty, List(("Location", s"$pathPrefix/users/$username/image"))), Done)
                            }
                          }
                        }
                      }
                        .recover {
                          case ex: Throwable => throw UC4Exception.InternalServerError("Error processing the image", ex.getMessage)
                        }
                  }
                }
                else {
                  throw UC4Exception.UnsupportedMediaType
                }
              case None => throw new UC4NonCriticalException(400, DetailedError(ErrorType.MissingHeader, Seq(SimpleError("Content-Type", "Missing"))))
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

  def createEnrollmentIdSecret(): String = {
    val rnd = new Random
    val bytes = new Array[Byte](enrollmentIdSecretByteLength)
    rnd.nextBytes(bytes)
    BaseEncoding.base64().encode(bytes)
  }

  /** Allows GET, POST */
  override def allowedGetPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, POST")

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows DELETE, GET, PUT */
  override def allowedDeleteGetPut: ServiceCall[NotUsed, Done] = allowedMethodsCustom("DELETE, GET, PUT")

  /** Allows DELETE */
  override def allowedDelete: ServiceCall[NotUsed, Done] = allowedMethodsCustom("DELETE")

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
