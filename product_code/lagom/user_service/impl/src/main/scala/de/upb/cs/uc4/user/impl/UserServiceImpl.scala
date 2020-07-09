package de.upb.cs.uc4.user.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry, ReadSide}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.shared.client.{CustomException, DetailedError, SimpleError}
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{Accepted, Confirmation, Rejected, RejectedWithError}
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.UserState
import de.upb.cs.uc4.user.impl.commands._
import de.upb.cs.uc4.user.impl.events.{OnUserCreate, OnUserDelete, UserEvent}
import de.upb.cs.uc4.user.impl.readside.UserEventProcessor
import de.upb.cs.uc4.user.model.post.{PostMessageAdmin, PostMessageLecturer, PostMessageStudent}
import de.upb.cs.uc4.user.model.user._
import de.upb.cs.uc4.user.model.{GetAllUsersResponse, JsonRole, JsonUsername, Role}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/** Implementation of the UserService */
class UserServiceImpl(clusterSharding: ClusterSharding, persistentEntityRegistry: PersistentEntityRegistry,
                      readSide: ReadSide, processor: UserEventProcessor, cassandraSession: CassandraSession)
                     (implicit ec: ExecutionContext, auth: AuthenticationService) extends UserService {
  readSide.register(processor)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** Get all users from the database */
  override def getAllUsers: ServiceCall[NotUsed, GetAllUsersResponse] =
    authenticated[NotUsed, GetAllUsersResponse](AuthenticationRole.Admin) {
      ServerServiceCall { (header, notUsed) =>
        for {
          students <- getAllStudents.invokeWithHeaders(header, notUsed)
          lecturers <- getAllLecturers.invokeWithHeaders(header, notUsed)
          admins <- getAllAdmins.invokeWithHeaders(header, notUsed)
        } yield
          (ResponseHeader(200, MessageProtocol.empty, List()),
            GetAllUsersResponse(students._2, lecturers._2, admins._2))
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
            throw new CustomException(TransportErrorCode(404, 1003, "Error"),
              DetailedError("key not found", Seq[SimpleError](SimpleError("username", "A user with the given username does not exist."))))
        }
    }
  }

  /** Get all students from the database */
  override def getAllStudents: ServerServiceCall[NotUsed, Seq[Student]] =
    authenticated[NotUsed, Seq[Student]](AuthenticationRole.Admin) { _ =>
      getAll("students").map(_.map(user => user.asInstanceOf[Student]))
    }

  /** Add a new student to the database */
  override def addStudent(): ServiceCall[PostMessageStudent, Done] = ServerServiceCall { (header, user) =>
    addUser(user.authUser).invokeWithHeaders(header, user.student)
  }

  /** Get a specific student */
  override def getStudent(username: String): ServiceCall[NotUsed, Student] =
    authenticated[NotUsed, Student](AuthenticationRole.All: _*) {
      _ =>
        getUser(username).invoke().map(user => user.role match {
          case Role.Student => user.asInstanceOf[Student]
          case _ => throw new CustomException(TransportErrorCode(400, 1003, "Error"),
            DetailedError("wrong object", Seq[SimpleError](SimpleError("role", "The user with the given username is not a student."))))
        })
    }

  /** Update an existing student */
  override def updateStudent(username: String): ServiceCall[Student, Done] =
    identifiedAuthenticated(AuthenticationRole.Student, AuthenticationRole.Admin) {
      (authUsername, role)=>
        ServerServiceCall { (header, user) =>
          if (username != user.username.trim){
            throw new CustomException(TransportErrorCode(400, 1003, "Error"), DetailedError("path parameter mismatch", List(SimpleError("username", "Username in object and username in path must match."))))
          }
          if (role == AuthenticationRole.Student && authUsername != user.username.trim){
            //TODO change to forbidden
            throw new CustomException(TransportErrorCode(403, 1003, "Error"), DetailedError("Generic Error", List(SimpleError("username", "A non-admin can only update their own profile."))))
          }
          updateUser().invokeWithHeaders(header, user)
        }
    }

  /** Get all lecturers from the database */
  override def getAllLecturers: ServerServiceCall[NotUsed, Seq[Lecturer]] =
    authenticated[NotUsed, Seq[Lecturer]](AuthenticationRole.Admin) { _ =>
      getAll("lecturers").map(_.map(user => user.asInstanceOf[Lecturer]))
    }

  /** Add a new lecturer to the database */
  override def addLecturer(): ServiceCall[PostMessageLecturer, Done] = ServerServiceCall { (header, user) =>
    addUser(user.authUser).invokeWithHeaders(header, user.lecturer)
  }

  /** Get a specific lecturer */
  override def getLecturer(username: String): ServiceCall[NotUsed, Lecturer] =
    authenticated[NotUsed, Lecturer](AuthenticationRole.All: _*) {
      _ =>
        getUser(username).invoke().map(user => user.role match {
          case Role.Lecturer => user.asInstanceOf[Lecturer]
          case _ => throw new CustomException(TransportErrorCode(400, 1003, "Error"),
            DetailedError("wrong object", Seq[SimpleError](SimpleError("role", "The user with the given username is not a lecturer."))))
        })
    }

  /** Update an existing lecturer */
  override def updateLecturer(username: String): ServiceCall[Lecturer, Done] =
    identifiedAuthenticated(AuthenticationRole.Lecturer, AuthenticationRole.Admin) {
      (authUsername, role)=>
        ServerServiceCall { (header, user) =>
          if (username != user.username.trim){
            throw new CustomException(TransportErrorCode(400, 1003, "Error"), DetailedError("path parameter mismatch", List(SimpleError("username", "Username in object and username in path must match."))))
          }
          if (role == AuthenticationRole.Lecturer && authUsername != user.username.trim){
            //TODO change to forbidden
            throw new CustomException(TransportErrorCode(403, 1003, "Error"), DetailedError("Generic Error", List(SimpleError("username", "A non-admin can only update their own profile."))))
          }
          updateUser().invokeWithHeaders(header, user)
        }
    }

  /** Get all admins from the database */
  override def getAllAdmins: ServerServiceCall[NotUsed, Seq[Admin]] =
    authenticated[NotUsed, Seq[Admin]](AuthenticationRole.Admin) { _ =>
      getAll("admins").map(_.map(user => user.asInstanceOf[Admin]))
    }

  /** Add a new admin to the database */
  override def addAdmin(): ServiceCall[PostMessageAdmin, Done] = ServerServiceCall { (header, user) =>
    addUser(user.authUser).invokeWithHeaders(header, user.admin)
  }

  /** Get a specific admin */
  override def getAdmin(username: String): ServiceCall[NotUsed, Admin] =
    authenticated[NotUsed, Admin](AuthenticationRole.All: _*) {
      _ =>
        getUser(username).invoke().map(user => user.role match {
          case Role.Admin => user.asInstanceOf[Admin]
          case _ => throw new CustomException(TransportErrorCode(400, 1003, "Error"),
            DetailedError("wrong object", Seq[SimpleError](SimpleError("role", "The user with the given username is not an admin."))))
        })
    }

  /** Update an existing admin */
  override def updateAdmin(username: String): ServiceCall[Admin, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin) {
      (authUsername, role)=>
        ServerServiceCall { (header, user) =>
          if (username != user.username.trim){
            throw new CustomException(TransportErrorCode(400, 1003, "Error"), DetailedError("path parameter mismatch", List(SimpleError("username", "Username in object and username in path must match."))))
          }
          updateUser().invokeWithHeaders(header, user)
        }
    }

  /** Get role of the user */
  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] =
    authenticated[NotUsed, JsonRole](AuthenticationRole.All: _*) { _ =>
      getUser(username).invoke().map(user => JsonRole(user.role))
    }

  /** Allows GET, PUT */
  override def allowedGetPut: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, PUT")

  /** Allows GET, POST */
  override def allowedGetPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, POST")

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows DELETE */
  override def allowedDelete: ServiceCall[NotUsed, Done] = allowedMethodsCustom("DELETE")

  /** Helper method for adding a generic User, independent of the role */
  private def addUser(authenticationUser: AuthenticationUser): ServerServiceCall[User, Done] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall { (_, user) =>
      val ref = entityRef(user.username)

      ref.ask[Confirmation](replyTo => CreateUser(user, authenticationUser, replyTo))
        .map {
          case Accepted => // Creation Successful
            (ResponseHeader(201, MessageProtocol.empty, List()), Done)
          case RejectedWithError(code, errorResponse) =>
            throw new CustomException(TransportErrorCode(code, 1003, "Error"), errorResponse)
        }
    }
  }

  /** Helper method for updating a generic User, independent of the role */
  private def updateUser(): ServerServiceCall[User, Done] = ServerServiceCall { (_, user) =>
    val ref = entityRef(user.username)

    ref.ask[Confirmation](replyTo => UpdateUser(user, replyTo))
      .map {
        case Accepted => // Update Successful
          (ResponseHeader(200, MessageProtocol.empty, List()), Done)
        case RejectedWithError(code, errorResponse) =>
          throw new CustomException(TransportErrorCode(code, 1003, "Error"), errorResponse)
      }
  }

  /** Helper method for getting a generic User, independent of the role */
  private def getUser(username: String): ServiceCall[NotUsed, User] = ServiceCall { _ =>
    val ref = entityRef(username)

    ref.ask[Option[User]](replyTo => GetUser(replyTo)).map {
      case Some(user) => user
      case None => throw new CustomException(TransportErrorCode(404, 1003, "Error"),
        DetailedError("key not found", Seq[SimpleError](SimpleError("username", "A user with the given username does not exist."))))
    }
  }

  /** Helper method for getting all Users */
  private def getAll(table: String): Future[Seq[User]] = {
    cassandraSession.selectAll(s"SELECT * FROM $table ;")
      .map(seq => seq
        .map(row => row.getString("username")) //Future[Seq[String]]
        .map(entityRef(_).ask[Option[User]](replyTo => GetUser(replyTo))) //Future[Seq[Future[Option[User]]]]
      )
      .flatMap(seq => Future.sequence(seq) //Future[Seq[Option[User]]]
        .map(seq => seq
          .filter(opt => opt.isDefined) //Get only existing users
          .map(opt => opt.get) //Future[Seq[User]]
        )
      )
  }

  /** Publishes every authentication change of a user */
  def userAuthenticationTopic(): Topic[AuthenticationUser] = TopicProducer.singleStreamWithOffset { fromOffset =>
    persistentEntityRegistry
      .eventStream(UserEvent.Tag, fromOffset)
      .mapConcat {
        //Filter only OnUserCreate events
        case EventStreamElement(_, OnUserCreate(_, authenticationUser), offset) =>
          immutable.Seq((authenticationUser, offset))
        case _ => Nil
      }
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
}