package de.upb.cs.uc4.matriculation.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.JsonUtil._
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand }
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionMatriculation
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionMatriculationTrait
import de.upb.cs.uc4.hyperledger.{ HyperledgerActorFactory, HyperledgerActorObject }
import de.upb.cs.uc4.matriculation.impl.commands.{ AddEntryToMatriculationData, AddMatriculationData, GetMatriculationData }
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.shared.server.messages.Accepted

import scala.util.Success

class MatriculationBehaviour(val config: Config) extends HyperledgerActorFactory[ConnectionMatriculationTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionMatriculationTrait =
    ConnectionMatriculation(username, channel, chaincode, walletPath, networkDescriptionPath)

  /** Gets called every time when the actor receives a command
    * Errors which this method will thrown will be handled accordingly
    * if the command implements [[HyperledgerReadCommand]] or the
    * [[HyperledgerWriteCommand]].
    *
    * @param connection the current active connection
    * @param command which should get executed
    */
  override protected def applyCommand(connection: ConnectionMatriculationTrait, command: HyperledgerCommand): Unit = command match {

    case AddEntryToMatriculationData(matriculationId, fieldOfStudy, semester, replyTo) =>
      connection.addEntryToMatriculationData(matriculationId, fieldOfStudy, semester)
      replyTo ! Accepted

    case AddMatriculationData(data, replyTo) =>
      connection.addMatriculationData(data.toJson)
      replyTo ! Accepted

    case GetMatriculationData(matriculationId, replyTo) =>
      replyTo ! Success(connection.getMatriculationData(matriculationId).fromJson[ImmatriculationData])
  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = MatriculationBehaviour
}

object MatriculationBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerCommand] = EntityTypeKey[HyperledgerCommand]("uc4matriculation")
  /** The reference to the entity */
  override val entityId: String = "matriculation"
}
