package de.upb.cs.uc4.hyperledger

import de.upb.cs.uc4.hyperledger.exceptions.traits.{ HyperledgerExceptionTrait, TransactionExceptionTrait }
import de.upb.cs.uc4.shared.client.exceptions._
import play.api.libs.json.{ Format, Json }

object HyperledgerUtils {

  implicit class ExceptionUtils(val exception: Exception) {

    def toCustomException: CustomException = {
      exception match {
        case _: HyperledgerExceptionTrait =>
          CustomException.InternalServerError
        case transactionEx: TransactionExceptionTrait =>
          errorMatching(Json.parse(transactionEx.payload).as[CustomError])
        case _ =>
          exception.printStackTrace()
          CustomException.InternalServerError
      }
    }
  }

  private def errorMatching(customError: CustomError): CustomException = {
    customError match {
      case genericError: GenericError => genericError.`type` match {
        case ErrorType.HLUnknownTransactionId => new CustomException(500, genericError)
        case ErrorType.HLNotFound => new CustomException(404, genericError)
        case ErrorType.HLConflict => new CustomException(409, genericError)
        case ErrorType.HLUnprocessableLedgerState => new CustomException(500, genericError)
        case _ => CustomException.InternalServerError
      }
      case detailedError: DetailedError => detailedError.`type` match {
        case ErrorType.HLUnprocessableEntity => new CustomException(422, detailedError)
        case _ => CustomException.InternalServerError
      }
      case transactionError: TransactionError => transactionError.`type` match {
        case ErrorType.HLInvalidTransactionCall => new CustomException(500, transactionError)
        case _ => CustomException.InternalServerError
      }
    }
  }

  object JsonUtil {

    implicit class ToJsonUtil[Type](val obj: Type)(implicit format: Format[Type]) {
      def toJson: String = Json.stringify(Json.toJson(obj))
    }

    implicit class FromJsonUtil(val json: String) {
      def fromJson[Type](implicit format: Format[Type]): Type = Json.parse(json).as[Type]
    }
  }
}
