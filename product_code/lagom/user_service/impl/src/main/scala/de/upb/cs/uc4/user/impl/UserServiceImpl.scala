package de.upb.cs.uc4.user.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.UserState
import de.upb.cs.uc4.user.impl.commands.UserCommand
import de.upb.cs.uc4.user.impl.readside.UserEventProcessor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/** Implementation of the UserService */
abstract class UserServiceImpl(clusterSharding: ClusterSharding,
                      readSide: ReadSide, processor: UserEventProcessor, cassandraSession: CassandraSession)
                     (implicit ec: ExecutionContext, auth: AuthenticationService) extends UserService {
  readSide.register(processor)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)
}
