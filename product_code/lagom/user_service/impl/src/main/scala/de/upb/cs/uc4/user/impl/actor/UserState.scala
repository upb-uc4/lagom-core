package de.upb.cs.uc4.user.impl.actor


import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
import de.upb.cs.uc4.shared.messages._
import de.upb.cs.uc4.user.impl.UserApplication
import de.upb.cs.uc4.user.impl.commands._
import de.upb.cs.uc4.user.impl.events.{OnUserCreate, OnUserDelete, OnUserUpdate, UserEvent}
import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.user.AuthenticationUser
import play.api.libs.json.{Format, Json}

/** The current state of a User */
case class UserState(optUser: Option[User]) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: UserCommand): ReplyEffect[UserEvent, UserState] =
    cmd match {
      case GetUser(replyTo) => Effect.reply(replyTo)(optUser)

      case CreateUser(user, authenticationUser, replyTo) =>
    
        val trimmedUser = user.trim
        val validationErrors = validateUser(trimmedUser,Some(authenticationUser))
        if (optUser.isEmpty){
          if (validationErrors.isEmpty) {
            Effect.persist(OnUserCreate(user, authenticationUser)).thenReply(replyTo) { _ => Accepted }
          }
          else {
            Effect.reply(replyTo)(RejectedWithError(422, DetailedError("validation error", "Your request parameters did not validate", validationErrors)))
          }
        }
        else {
          Effect.reply(replyTo)(RejectedWithError(409, DetailedError("key value error", "Username is already taken", List(SimpleError("username", "Username already exists")))))
        }
  
        
      case UpdateUser(user, replyTo) =>
        val trimmedUser = user.trim
        val validationErrors = validateUser(trimmedUser,None)

        if (optUser.isDefined) {
          if(validationErrors.isEmpty){
            Effect.persist(OnUserUpdate(user)).thenReply(replyTo) { _ => Accepted }
          } else {
            Effect.reply(replyTo)(RejectedWithError(422, DetailedError("validation error", "Your request parameters did not validate", validationErrors)))
          }
        } else {
          Effect.reply(replyTo)(RejectedWithError(404, DetailedError("key value error", "Username not found", List(SimpleError("username", "Username does not exist")))))
      }

        
      case DeleteUser(replyTo) =>
        if (optUser.isDefined) {
          Effect.persist(OnUserDelete(optUser.get)).thenReply(replyTo) { _ => Accepted }
        } else {
          Effect.reply(replyTo)(Rejected("A user with the given username does not exist."))
        }

      case _ =>
        println("Unknown Command")
        Effect.noReply
    }


  /** Functions as an EventHandler
    *
    * @param evt the given event
    */
  def applyEvent(evt: UserEvent): UserState =
    evt match {
      case OnUserCreate(user, _) =>
        copy(Some(user))
      case OnUserUpdate(user) => copy(Some(user))
      case OnUserDelete(_) => copy(None)
      case _ =>
        println("Unknown Event")
        this
    }


  def validateUser(user: User, authenticationUserOpt: Option[AuthenticationUser]): Seq[SimpleError] = {
    val generalRegex = """[\s\S]+""".r // Allowed characters for general strings TBD
    // TODO: More REGEXes need to be defined to check firstname etc. But it is not clear from the API
    val usernameRegex = """[a-zA-Z0-9-]+""".r
    val nameRegex = """[a-zA-Z]+""".r
    val mailRegex = """[a-zA-Z0-9\Q.-_,\E]+@[a-zA-Z0-9\Q.-_,\E]+\.[a-zA-Z]+""".r
    val fos = List("Computer Science","Philosophy","Media Sciences", "Economics", "Mathematics", "Physics", "Chemistry",
      "Education", "Sports Science", "Japanology", "Spanish Culture", "Pedagogy", "Business Informatics", "Linguistics")

    var error= List[SimpleError]()

    if (!usernameRegex.matches(user.getUsername)) {
      error :+= SimpleError("username","Username may only contain [..].")
    }
    if (authenticationUserOpt.isDefined && authenticationUserOpt.get.password.trim == ""){
      error :+= SimpleError("password","Password must not be empty.")
    }
    if (optUser.isEmpty && !Role.All.contains(user.role)) { //optUser check to ensure this is during creation
      error :+= (SimpleError("role", "Role must be one of [..]" + Role.All + "."))
    }
    if (user.getAddress.oneEmpty) {
      error :+= (SimpleError("address", "Address fields must not be empty."))
    }
    if (!mailRegex.matches(user.getEmail)) {
      error :+= (SimpleError("email", "Email must be in email format example@xyz.com."))
    }
    if (!nameRegex.matches(user.getFirstName)) {
      error :+= (SimpleError("firstName", "First name must not contain XYZ."))
    }
    if (!nameRegex.matches(user.getLastName)) {
      error :+= (SimpleError("lastName", "First name must not contain XYZ."))
    }
    if (!generalRegex.matches(user.getPicture)) { //TODO, this does not make any sense, but pictures are not defined yet
      error :+= SimpleError("picture", "Picture is invalid.")
    }

    user match {
      case u if u.optStudent.isDefined =>
        val s = u.student
        if(!(s.matriculationId forall Character.isDigit) || !(s.matriculationId.toInt > 0)) {
          error :+= SimpleError("matriculationId", "Student ID invalid.")
        }
        if(!(s.semesterCount > 0)) {
          error :+= SimpleError("semesterCount", "Semester count must be a positive integer.")
        }
        if(!s.fieldsOfStudy.forall(fos.contains)) {
          error :+= SimpleError("fieldsOfStudy", "Fields of Study must be one of [..].")
        }
      case u if u.optLecturer.isDefined =>
        val l = u.lecturer
        if (!generalRegex.matches(l.freeText)) {
          error :+= SimpleError("freeText", "Free text must only contain the following characters: [..].")
        }
        if (!generalRegex.matches(l.researchArea)) {
          error :+= SimpleError("researchArea", "Research area must only contain the following characters [..].")
        }
      case _ =>
    }
    error.toSeq
  }
}

object UserState {

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: UserState = UserState(None)

  /**
    * The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey: EntityTypeKey[UserCommand] = EntityTypeKey[UserCommand](UserApplication.cassandraOffset)

  /**
    * Format for the course state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[UserState] = Json.format
}
