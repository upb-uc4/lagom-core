package de.upb.cs.uc4.hyperledger.commands

/** The trait for the commands needed in the state
  * Every command is a case class containing the
  * necessary information to execute the command
  */
trait HyperledgerCommand extends HyperledgerCommandSerializable
