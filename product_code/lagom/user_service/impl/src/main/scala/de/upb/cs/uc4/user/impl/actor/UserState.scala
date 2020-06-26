package de.upb.cs.uc4.user.impl.actor


import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
import de.upb.cs.uc4.shared.messages.{Accepted, Rejected}
import de.upb.cs.uc4.user.impl.UserApplication
import de.upb.cs.uc4.user.impl.commands.{CreateUser, DeleteUser, GetUser, UpdateUser, UserCommand}
import de.upb.cs.uc4.user.impl.events.{OnUserCreate, OnUserDelete, OnUserUpdate, UserEvent}
import play.api.libs.json.{Format, Json}
import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.user.AuthenticationUser

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
      val responseCode = validateUserSyntax(trimmedUser,Some(authenticationUser))
      if (optUser.isEmpty){
        if (responseCode == "valid" ) {
          Effect.persist(OnUserCreate(user, authenticationUser)).thenReply(replyTo) { _ => Accepted }
        }
        else {
          Effect.reply(replyTo)(Rejected(responseCode))
        }
      }
      else {
        Effect.reply(replyTo)(Rejected("A user with the given username already exists."))
      }
  
        
      case UpdateUser(user, replyTo) =>
        val trimmedUser = user.trim
        val responseCode = validateUserSyntax(trimmedUser,None)

        if (optUser.isDefined) {
          if(responseCode == "valid"){
           Effect.persist(OnUserUpdate(user)).thenReply(replyTo) { _ => Accepted }
          } else {
           Effect.reply(replyTo)(Rejected(responseCode))
          }
        } else {
         Effect.reply(replyTo)(Rejected("A user with the given username does not exist."))
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
      case OnUserCreate(user, _) => copy(Some(user))
      case OnUserUpdate(user) => copy(Some(user))
      case OnUserDelete(_) => copy(None)
      case _ =>
        println("Unknown Event")
        this
    }

    
  def validateUserSyntax(user: User, authenticationUserOpt: Option[AuthenticationUser]): String = {
    val generalRegex = """[\s\S]+""".r // Allowed characters for general strings "[a-zA-Z0-9\\s]+".r TBD
    // More REGEXes need to be defined to check firstname etc. But it is not clear from the API
    val usernameRegex = """[a-zA-Z0-9-]+""".r
    val nameRegex = """[a-zA-Z]+""".r
    val mailRegex = """[a-zA-Z0-9\Q.-_,\E]+@[a-zA-Z0-9\Q.-_,\E]+\.[a-zA-Z]+""".r
    val fos = List("Computer Science", "Gender Studies", "Electrical Engineering")
    
    if (authenticationUserOpt.isDefined && authenticationUserOpt.get.password.trim == ("")){
      return "10"
    } 
    user match {
      case u if (!usernameRegex.matches(user.getUsername)) =>
        "01" // username must only contain [..]
      case u if (!optUser.isDefined && !Role.All.contains(u.role)) =>
        "20" // role must be one of [..]; Only for create
      case u if (user.getAddress.oneEmpty) => 
        "30" //	address fields must not be empty
      case u if (!mailRegex.matches(u.getEmail)) =>
        "40" // email format invalid
      case u if (!nameRegex.matches(user.getFirstName)) =>
        "50" // first name can only contain letters
      case u if (!nameRegex.matches(user.getLastName)) =>
        "60" // first name can only contain letters
      case u if (!generalRegex.matches(user.getPicture)) => //TODO, this does not make any sense, but pictures are not defined yet
        "70" // picture invalid
      case u if (!(u.optStudent == None)) => 
        u.student match {
          case s if(!(s.matriculationId forall Character.isDigit) || !(s.matriculationId.toInt > 0)) =>
            "100" // matriculation ID invalid
          case s if(!(s.semesterCount > 0)) =>
            "110" // semester count must be a positive integer
          case s if(!s.fieldsOfStudy.forall(fos.contains)) => 
            "120" // fields of study must be one of the defined fields of study
          case _=> "valid"
        }
      case u if (!(u.optLecturer == None)) =>
        u.lecturer match {
          case l if (!generalRegex.matches(l.freeText)) => 
            "200" //	free text must only contain the following characters
          case l if (!generalRegex.matches(l.researchArea)) => 
            "210" // 	research area must only contain the following characters
          case _=> "valid"
        }
      case _ => "valid"
    }

    
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
