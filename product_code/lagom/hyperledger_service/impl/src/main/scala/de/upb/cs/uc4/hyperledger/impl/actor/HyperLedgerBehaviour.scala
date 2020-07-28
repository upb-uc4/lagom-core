package de.upb.cs.uc4.hyperledger.impl.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import de.upb.cs.uc4.hyperledger.impl.HyperLedgerApplication
import de.upb.cs.uc4.hyperledger.impl.commands.{HyperLedgerCommand, Read, Shutdown, Write}
import de.upb.cs.uc4.hyperledger.traits.{ChaincodeActionsTrait, ConnectionManagerTrait}
import de.upb.cs.uc4.shared.server.messages.{Accepted, Rejected}

import scala.util.{Failure, Success}

object HyperLedgerBehaviour {

  def create(manager: ConnectionManagerTrait): Behavior[HyperLedgerCommand] = Behaviors.setup { _ =>

    val chaincodeConnection: ChaincodeActionsTrait = manager.createConnection()

    def start(): Behavior[HyperLedgerCommand] =
      Behaviors.receive {
        case (_, cmd) =>
          cmd match {
            case Read(transactionId, params, replyTo) =>
              try {
                replyTo ! Success(chaincodeConnection.evaluateTransaction(transactionId, params: _*))
              } catch {
                case e: Exception =>
                  replyTo ! Failure(e)
              }
              Behaviors.same

            case Write(transactionId, params, replyTo) =>
              try {
                chaincodeConnection.submitTransaction(transactionId, params: _*)
                replyTo ! Accepted
              } catch {
                //TODO Catch HyperledgerException
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
