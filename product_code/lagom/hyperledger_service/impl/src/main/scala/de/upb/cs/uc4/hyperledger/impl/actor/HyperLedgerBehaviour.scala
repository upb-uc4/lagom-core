package de.upb.cs.uc4.hyperledger.impl.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import de.upb.cs.uc4.hyperledger.ConnectionManagerTrait
import de.upb.cs.uc4.hyperledger.impl.HyperLedgerApplication
import de.upb.cs.uc4.hyperledger.impl.commands.{HyperLedgerCommand, Read, Shutdown, Write}
import de.upb.cs.uc4.hyperledger.traits.{ChaincodeTrait, ConnectionManagerTrait}
import de.upb.cs.uc4.shared.messages.{Accepted, Rejected}

object HyperLedgerBehaviour {

  def create(manager: ConnectionManagerTrait): Behavior[HyperLedgerCommand] = Behaviors.setup { _ =>

    val chaincodeConnection: ChaincodeTrait = manager.createConnection()

    def start(): Behavior[HyperLedgerCommand] =
      Behaviors.receive {
        case (_, cmd) =>
          cmd match {
            case Read(key, replyTo) =>
              try {
                replyTo ! Some(chaincodeConnection.getCourseById(key))
              } catch {
                case _: Exception =>
                  replyTo ! None
              }
              Behaviors.same

            case Write(json, replyTo) =>
              try {
                chaincodeConnection.addCourse(json)
                replyTo ! Accepted
              } catch {
                case e: Exception =>
                  replyTo ! Rejected(e.getMessage)
              }
              Behaviors.same

            case Shutdown() =>
              if(chaincodeConnection !=  null){
                chaincodeConnection.close()
              }
              Behaviors.same
          }
      }

    start()
  }

  val typeKey: EntityTypeKey[HyperLedgerCommand] = EntityTypeKey[HyperLedgerCommand](HyperLedgerApplication.cassandraOffset)
}
