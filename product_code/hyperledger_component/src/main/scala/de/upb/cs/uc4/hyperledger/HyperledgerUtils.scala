package de.upb.cs.uc4.hyperledger

import de.upb.cs.uc4.hyperledger.exceptions.traits.{ HyperledgerExceptionTrait, TransactionExceptionTrait }
import de.upb.cs.uc4.shared.client.exceptions._
import play.api.libs.json.{ Format, Json }

object HyperledgerUtils {

  implicit class ExceptionUtils(val exception: Throwable) {

    def toCustomException: UC4Exception = {
      exception match {
        case exp: HyperledgerExceptionTrait =>
          UC4Exception.InternalServerError(
            s"HyperledgerException thrown by ${exp.transactionId}",
            exp.innerException.getMessage
          )
        case transactionEx: TransactionExceptionTrait =>
          errorMatching(Json.parse(transactionEx.payload).as[UC4Error])
        case exp: Throwable =>
          UC4Exception.InternalServerError("Unknown Internal Error", exp.getMessage)
      }
    }
  }

  private def errorMatching(customError: UC4Error): UC4Exception = {
    customError match {
      case genericError: GenericError => genericError.`type` match {
        case ErrorType.HLUnknownTransactionId => new UC4Exception(500, genericError)
        case ErrorType.HLNotFound => new UC4Exception(404, genericError)
        case ErrorType.HLConflict => new UC4Exception(409, genericError)
        case ErrorType.HLUnprocessableLedgerState => new UC4Exception(500, genericError)
        case _ => UC4Exception.InternalServerError("Unknown GenericError", "Hyperledger uses a new ErrorType")
      }
      case detailedError: DetailedError => detailedError.`type` match {
        case ErrorType.HLUnprocessableEntity => new UC4Exception(422, detailedError)
        case _ => UC4Exception.InternalServerError("Unknown DeteilaedError", "Hyperledger uses a new ErrorType")
      }
      case transactionError: TransactionError => transactionError.`type` match {
        case ErrorType.HLInvalidTransactionCall => new UC4Exception(500, transactionError)
        case _ => UC4Exception.InternalServerError("Unknown TransactionError", "Hyperledger uses a new ErrorType")
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
