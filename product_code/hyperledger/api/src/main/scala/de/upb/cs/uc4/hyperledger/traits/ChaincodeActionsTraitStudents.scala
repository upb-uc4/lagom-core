package de.upb.cs.uc4.hyperledger.traits

import de.upb.cs.uc4.hyperledger.exceptions.TransactionErrorException
import org.hyperledger.fabric.gateway.Contract

/**
 * Trait to provide explicit access to chaincode transactions regarding courses
 */
trait ChaincodeActionsTraitStudents extends ChaincodeActionsTraitInternal {

  def contract_student: Contract

  /**
    * Submits any transaction specified by transactionId.
    *
    * @param transactionId transactionId to submit
    * @param params        parameters to pass to the transaction
    * @throws Exception if chaincode throws an exception.
    * @return success_state
    */
  @throws[Exception]
  private def internalSubmitTransaction(transactionId: String, params: String*): Array[Byte] =
    this.internalSubmitTransaction(contract_student, transactionId, params: _*)

  /**
    * Evaluates the transaction specified by transactionId.
    *
    * @param transactionId transactionId to evaluate
    * @param params        parameters to pass to the transaction
    * @throws Exception if chaincode throws an exception.
    * @return success_state
    */
  @throws[Exception]
  private def internalEvaluateTransaction(transactionId: String, params: String*): Array[Byte] =
    this.internalEvaluateTransaction(contract_student, transactionId, params: _*)

  /**
   * Executes the "addCourse" query.
   *
   * @param jSonMatriculationData Information about the matriculation to add.
   * @throws Exception if chaincode throws an exception.
   * @return Success_state
   */
  @throws[Exception]
  final def addMatriculationData(jSonMatriculationData : String): String = {
    wrapTransactionResult("addMatriculationData",
      this.internalSubmitTransaction("addMatriculationData", jSonMatriculationData))
  }

  /**
   * Submits the "deleteCourseById" query.
   *
   * @param matriculationId courseId to add entry to
   * @param fieldOfStudy field of study the student enroled in
   * @param semester the semester the student enrolled for
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  final def addEntryToMatriculationData(matriculationId : String, fieldOfStudy : String, semester : String): String = {
    wrapTransactionResult("addEntryToMatriculationData",
      this.internalSubmitTransaction("addEntryToMatriculationData", matriculationId, fieldOfStudy, semester))
  }

  /**
   * Submits the "updateCourseById" query.
   *
   * @param jSonMatriculationData matriculationInfo to update
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  final def updateMatriculationData(jSonMatriculationData : String): String = {
    wrapTransactionResult("updateMatriculationData",
      this.internalSubmitTransaction("updateMatriculationData", jSonMatriculationData))
  }

  /**
   * Executes the "getCourseById" query.
   *
   * @param matId matriculationId to get information
   * @throws Exception if chaincode throws an exception.
   * @return JSon Course Object
   */
  @throws[Exception]
  final def getMatriculationData(matId: String): String = {
    wrapTransactionResult("getMatriculationData",
      this.internalEvaluateTransaction("getMatriculationData", matId))
  }
}
