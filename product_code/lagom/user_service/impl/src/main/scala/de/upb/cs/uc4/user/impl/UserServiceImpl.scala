package de.upb.cs.uc4.user.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, MessageProtocol, NotFound, ResponseHeader}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{PersistentEntityRegistry, ReadSide}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.shared.ServiceCallFactory._
import de.upb.cs.uc4.shared.messages.{Accepted, Confirmation, Rejected}
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.{User, UserState}
import de.upb.cs.uc4.user.impl.commands._
import de.upb.cs.uc4.user.impl.events.{OnUserCreate, OnUserDelete, UserEvent}
import de.upb.cs.uc4.user.impl.readside.UserEventProcessor
import de.upb.cs.uc4.user.model.post.{PostMessageAdmin, PostMessageLecturer, PostMessageStudent}
import de.upb.cs.uc4.user.model.user.{Admin, AuthenticationUser, Lecturer, Student}
import de.upb.cs.uc4.user.model.{GetAllUsersResponse, JsonRole, JsonUsername, Role}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

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
    authenticated[NotUsed, GetAllUsersResponse](Role.Admin) {
      ServerServiceCall { (header, notUsed) =>
        for {
          students <- getAllStudents.invokeWithHeaders(header, notUsed)
          lecturers <- getAllLecturers.invokeWithHeaders(header, notUsed)
          admins <- getAllAdmins.invokeWithHeaders(header, notUsed)
        } yield
          (ResponseHeader(200, MessageProtocol.empty, List(("1", "Operation successful"))) ,
            GetAllUsersResponse(students._2, lecturers._2, admins._2))
      }
    }

  /** Delete a users from the database */
  override def deleteUser(username: String): ServiceCall[NotUsed, Done] = authenticated(Role.Admin) {
    ServerServiceCall { (_, _) =>
      val ref = entityRef(username)

      ref.ask[Confirmation](replyTo => DeleteUser(replyTo))
        .map {
          case Accepted => // Update Successful
            (ResponseHeader(201, MessageProtocol.empty, List(("1", "Operation successful"))), Done)
          case Rejected("A user with the given username does not exist.") => // Already exists
            (ResponseHeader(409, MessageProtocol.empty, List(("1", "A user with the given username does not exist."))), Done)
        }
    }
  }

  /** Get all students from the database */
  override def getAllStudents: ServerServiceCall[NotUsed, Seq[Student]] = authenticated[NotUsed, Seq[Student]](Role.Admin) { _ =>
    getAll("students").map(_.map(_.student))
  }

  /** Add a new student to the database */
  override def addStudent(): ServiceCall[PostMessageStudent, Done] = ServerServiceCall { (header, user) =>
    addUser(user.authUser).invokeWithHeaders(header, User(user.student))
  }

  /** Get a specific student */
  override def getStudent(username: String): ServiceCall[NotUsed, Student] = authenticated[NotUsed, Student](Role.All: _*) {
    _ =>
      getUser(username).invoke().map(user => user.role match {
        case Role.Student => user.student
        case _ => throw BadRequest("Not a student")
      })
  }


  /** Deletes a student */
  override def deleteStudent(username: String): ServiceCall[NotUsed, Done] = deleteUser(username)

  /** Update an existing student */
  override def updateStudent(): ServiceCall[Student, Done] = authenticated[Student, Done](Role.Student, Role.Admin) {
    ServerServiceCall { (header, user) =>
      updateUser().invokeWithHeaders(header, User(user))
    }
  }

  /** Get all lecturers from the database */
  override def getAllLecturers: ServerServiceCall[NotUsed, Seq[Lecturer]] = authenticated[NotUsed, Seq[Lecturer]](Role.Admin) { _ =>
    getAll("lecturers").map(_.map(_.lecturer))
  }

  /** Add a new lecturer to the database */
  override def addLecturer(): ServiceCall[PostMessageLecturer, Done] = ServerServiceCall { (header, user) =>
    addUser(user.authUser).invokeWithHeaders(header, User(user.lecturer))
  }

  /** Get a specific lecturer */
  override def getLecturer(username: String): ServiceCall[NotUsed, Lecturer] = authenticated[NotUsed, Lecturer](Role.All: _*) {
    _ =>
      getUser(username).invoke().map(user => user.role match {
        case Role.Lecturer => user.lecturer
        case _ => throw BadRequest("Not a lecturer")
      })
  }

  /** Deletes a lecturer */
  override def deleteLecturer(username: String): ServiceCall[NotUsed, Done] = deleteUser(username)

  /** Update an existing lecturer */
  override def updateLecture(): ServiceCall[Lecturer, Done] = authenticated[Lecturer, Done](Role.Lecturer, Role.Admin) {
    ServerServiceCall { (header, user) =>
      updateUser().invokeWithHeaders(header, User(user))
    }
  }

  /** Get all admins from the database */
  override def getAllAdmins: ServerServiceCall[NotUsed, Seq[Admin]] = authenticated[NotUsed, Seq[Admin]](Role.Admin) { _ =>
    getAll("admins").map(_.map(_.admin))
  }

  /** Add a new admin to the database */
  override def addAdmin(): ServiceCall[PostMessageAdmin, Done] = ServerServiceCall { (header, user) =>
    addUser(user.authUser).invokeWithHeaders(header, User(user.admin))
  }

  /** Get a specific admin */
  override def getAdmin(username: String): ServiceCall[NotUsed, Admin] = authenticated[NotUsed, Admin](Role.All: _*) {
    _ =>
      getUser(username).invoke().map(user => user.role match {
        case Role.Admin => user.admin
        case _ => throw BadRequest("Not an admin")
      })
  }

  /** Deletes an admin */
  override def deleteAdmin(username: String): ServiceCall[NotUsed, Done] = deleteUser(username)

  /** Update an existing admin */
  override def updateAdmin(): ServiceCall[Admin, Done] = authenticated[Admin, Done](Role.Admin) {
    ServerServiceCall { (header, user) =>
      updateUser().invokeWithHeaders(header, User(user))
    }
  }

  /** Get role of the user */
  override def getRole(username: String): ServiceCall[NotUsed, JsonRole] =
    authenticated[NotUsed, JsonRole](Role.All: _*) { _ =>
      getUser(username).invoke().map(user => JsonRole(user.role))
    }

  /** Allows GET, PUT, DELETE */
  override def allowedGetPutDelete: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, PUT, DELETE")

  /** Allows GET, POST */
  override def allowedGetPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, POST")

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows DELETE */
  override def allowedDelete: ServiceCall[NotUsed, Done] = allowedMethodsCustom("DELETE")

  /** Helper method for adding a generic User, independent of the role */
  private def addUser(authenticationUser: AuthenticationUser): ServerServiceCall[User, Done] = authenticated(Role.Admin) {
    ServerServiceCall { (_, user) =>
      val ref = entityRef(user.getUsername)

      ref.ask[Confirmation](replyTo => CreateUser(user, authenticationUser, replyTo))
        .map {
          case Accepted => // Creation Successful
            (ResponseHeader(201, MessageProtocol.empty, List(("1", "Operation successful"))), Done)
          case Rejected("A user with the given username already exist.") => // Already exists
            (ResponseHeader(409, MessageProtocol.empty, List(("1", "A user with the given username already exist."))), Done)
        }
    }
  }
  /** Helper method for updating a generic User, independent of the role */
  private def updateUser(): ServerServiceCall[User, Done] = ServerServiceCall { (_, user) =>
    val ref = entityRef(user.getUsername)

    ref.ask[Confirmation](replyTo => UpdateUser(user, replyTo))
      .map {
        case Accepted => // Update Successful
          (ResponseHeader(201, MessageProtocol.empty, List(("1", "Operation successful"))), Done)
        case Rejected("A user with the given username does not exist.") => // Already exists
          (ResponseHeader(409, MessageProtocol.empty, List(("1", "A user with the given username does not exist."))), Done)
      }
  }
  /** Helper method for getting a generic User, independent of the role */
  private def getUser(username: String): ServiceCall[NotUsed, User] = ServiceCall { _ =>
    val ref = entityRef(username)

    ref.ask[Option[User]](replyTo => GetUser(replyTo)).map {
      case Some(user) => user
      case None => throw NotFound("Username was not found")
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
      .map(ev => ev.event match {
        case OnUserCreate(_, authenticationUser) => (authenticationUser, ev.offset)
      })
  }

  /** Publishes every deletion of a user */
  def userDeletedTopic(): Topic[JsonUsername] = TopicProducer.singleStreamWithOffset { fromOffset =>
    persistentEntityRegistry
      .eventStream(UserEvent.Tag, fromOffset)
      .map(ev => ev.event match {
        case OnUserDelete(user) => (JsonUsername(user.getUsername), ev.offset)
      })
  }
}