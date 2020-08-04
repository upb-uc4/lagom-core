package de.upb.cs.uc4.hyperledger.traits

import de.upb.cs.uc4.hyperledger.exceptions.{InvalidCallException, TransactionErrorException}

/**
 * Trait to provide general access to all chaincode transactions.
 * Aggregates all specific ChaincodeAccess traits.
 */
trait ChaincodeActionsTrait extends ChaincodeActionsTraitCourses with ChaincodeActionsTraitStudents {

  /**
   * Submits any transaction specified by transactionId.
   *
   * @param transactionId transactionId to submit
   * @param params        parameters to pass to the transaction
   * @throws InvalidCallException      if transactionId could not be mapped
   * @throws TransactionErrorException if an error occured during transaction
   * @return success_state
   */
  @throws[InvalidCallException]
  @throws[TransactionErrorException]
  final def submitTransaction(transactionId: String, params: String*): String = {
    transactionId match {
      case "addCourse" => {
        this.validateParameterCount(transactionId, 1, params.toArray)
        this.addCourse(params.apply(0))
      }
      case "deleteCourseById" => {
        this.validateParameterCount(transactionId, 1, params.toArray)
        this.deleteCourseById(params.apply(0))
      }
      case "updateCourseById" => {
        this.validateParameterCount(transactionId, 2, params.toArray)
        this.updateCourseById(params.apply(0), params.apply(1))
      }
      case "addMatriculationData" => {
        this.validateParameterCount(transactionId, 1, params.toArray)
        this.addMatriculationData(params.apply(0))
      }
      case "addEntryToMatriculationData" => {
        this.validateParameterCount(transactionId, 3, params.toArray)
        this.addEntryToMatriculationData(params.apply(0), params.apply(1), params.apply(2))
      }
      case "updateMatriculationData" => {
        this.validateParameterCount(transactionId, 1, params.toArray)
        this.updateMatriculationData(params.apply(0))
      }
      case _ => throw InvalidCallException.CreateInvalidIDException(transactionId)
    }
  }

  /**
   * Evaluates the transaction specified by transactionId.
   *
   * @param transactionId transactionId to evaluate
   * @param params        parameters to pass to the transaction
   * @throws InvalidCallException      if transactionId could not be mapped
   * @throws TransactionErrorException if an error occured during transaction
   * @return success_state
   */
  @throws[InvalidCallException]
  @throws[TransactionErrorException]
  final def evaluateTransaction(transactionId: String, params: String*): String = {
    transactionId match {
      case "getCourseById" => {
        this.validateParameterCount(transactionId, 1, params.toArray)
        this.getCourseById(params.apply(0))
      }
      case "getAllCourses" => {
        this.validateParameterCount(transactionId, 0, params.toArray)
        this.getAllCourses()
      }
      case "getMatriculationData" => {
        this.validateParameterCount(transactionId, 1, params.toArray)
        this.getMatriculationData(params.apply(0))
      }
      case _ => throw InvalidCallException.CreateInvalidIDException(transactionId)
    }
  }
}
