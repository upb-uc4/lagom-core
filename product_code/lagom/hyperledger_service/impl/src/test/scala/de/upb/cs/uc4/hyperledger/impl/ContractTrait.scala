package de.upb.cs.uc4.hyperledger.impl

import java.util.function.Consumer
import java.util.regex.Pattern

import org.hyperledger.fabric.gateway.spi.Checkpointer
import org.hyperledger.fabric.gateway.{Contract, ContractEvent, Transaction}

trait ContractTrait extends Contract{
  override def createTransaction(name: String): Transaction = null

  override def addContractListener(listener: Consumer[ContractEvent]): Consumer[ContractEvent] = null

  override def addContractListener(listener: Consumer[ContractEvent], eventName: String): Consumer[ContractEvent] = null

  override def addContractListener(listener: Consumer[ContractEvent], eventNamePattern: Pattern): Consumer[ContractEvent] = null

  override def addContractListener(checkpointer: Checkpointer, listener: Consumer[ContractEvent]): Consumer[ContractEvent] = null

  override def addContractListener(checkpointer: Checkpointer, listener: Consumer[ContractEvent], eventName: String): Consumer[ContractEvent] = null

  override def addContractListener(checkpointer: Checkpointer, listener: Consumer[ContractEvent], eventNamePattern: Pattern): Consumer[ContractEvent] = null

  override def addContractListener(startBlock: Long, listener: Consumer[ContractEvent]): Consumer[ContractEvent] = null

  override def addContractListener(startBlock: Long, listener: Consumer[ContractEvent], eventName: String): Consumer[ContractEvent] = null

  override def addContractListener(startBlock: Long, listener: Consumer[ContractEvent], eventNamePattern: Pattern): Consumer[ContractEvent] = null

  override def removeContractListener(listener: Consumer[ContractEvent]): Unit = null
}
