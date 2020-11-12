package de.upb.cs.uc4.user.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.image.api.ImageProcessingService
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionComponent
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.impl.actor.{ UserBehaviour, UserState }
import de.upb.cs.uc4.user.impl.readside.{ UserDatabase, UserEventProcessor }
import play.api.db.HikariCPComponents

abstract class UserApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with KafkaEncryptionComponent {

  // Create ReadSide
  lazy val database: UserDatabase = wire[UserDatabase]
  lazy val processor: UserEventProcessor = wire[UserEventProcessor]
  readSide.register(processor)

  // Bind  services
  lazy val authentication: AuthenticationService = serviceClient.implement[AuthenticationService]
  lazy val imageProcessing: ImageProcessingService = serviceClient.implement[ImageProcessingService]

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[UserService](wire[UserServiceImpl])
    .additionalRouter(new ImageUploadRouter(this.defaultActionBuilder, this.playBodyParsers, this).router)

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = UserSerializerRegistry

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(UserState.typeKey)(
      entityContext => UserBehaviour.create(entityContext)
    )
  )
}

object UserApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Users"
}
