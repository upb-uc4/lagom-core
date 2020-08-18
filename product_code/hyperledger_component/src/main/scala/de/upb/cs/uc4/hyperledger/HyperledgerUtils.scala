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
        case "hl: unknown transactionId"      => new CustomException(500, genericError)
        case "hl: unprocessable entity"       => new CustomException(422, genericError)
        case "hl: not found"                  => new CustomException(404, genericError)
        case "hl: conflict"                   => new CustomException(409, genericError)
        case "hl: unprocessable ledger state" => new CustomException(500, genericError)
      }
      case detailedError: DetailedError => detailedError.`type` match {
        case "hl: unprocessable field" => new CustomException(422, detailedError)
      }
      case transactionError: TransactionError => transactionError.`type` match {
        case "hl: invalid transaction call" => new CustomException(500, transactionError)
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
