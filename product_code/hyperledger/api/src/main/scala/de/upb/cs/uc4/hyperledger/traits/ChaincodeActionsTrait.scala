package de.upb.cs.uc4.hyperledger.traits

import de.upb.cs.uc4.hyperledger.exceptions.{InvalidTransactionException, TransactionErrorException}

/**
 * Trait to provide general access to all chaincode transactions.
 * Aggregates all specific ChaincodeAccess traits.
 */
trait ChaincodeActionsTrait extends ChaincodeActionsTraitCourses {

  /**
   * Submits any transaction specified by transactionId.
   * @param transactionId transactionId to submit
   * @param params parameters to pass to the transaction
   * @throws InvalidTransactionException if transactionId could not be mapped
   * @throws TransactionErrorException if an error occured during transaction
   * @return success_state
   */
  @throws[InvalidTransactionException]
  @throws[TransactionErrorException]
  final def submitTransaction(transactionId : String, params : String*) : String = {
    transactionId match {
      case "addCourse" => this.addCourse(params.apply(0))
      case "deleteCourseById" => this.deleteCourseById(params.apply(0))
      case "updateCourseById" => this.updateCourseById(params.apply(0), params.apply(1))
      case _ => throw new InvalidTransactionException(transactionId)
    }
  }

  /**
   * Evaluates the transaction specified by transactionId.
   * @param transactionId transactionId to evaluate
   * @param params parameters to pass to the transaction
   * @throws InvalidTransactionException if transactionId could not be mapped
   * @throws TransactionErrorException if an error occured during transaction
   * @return success_state
   */
  @throws[InvalidTransactionException]
  @throws[TransactionErrorException]
  final def evaluateTransaction(transactionId : String, params : String*) : String = {
    transactionId match {
      case "getCourseById" => this.getCourseById(params.apply(0))
      case "getAllCourses" => this.getAllCourses()
      case _ => throw new InvalidTransactionException(transactionId)
    }
  }
}
