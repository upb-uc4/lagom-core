package de.upb.cs.uc4.hyperledger

import de.upb.cs.uc4.hyperledger.exceptions.traits.{ HyperledgerExceptionTrait, NetworkExceptionTrait, TransactionExceptionTrait }
import de.upb.cs.uc4.shared.client.exceptions._
import play.api.libs.json.{ Format, JsResultException, Json }

object HyperledgerUtils {

  implicit class ExceptionUtils(val exception: Throwable) {

    def toUC4Exception: UC4Exception = {
      exception match {
        case exp: HyperledgerExceptionTrait =>
          UC4Exception.InternalServerError(
            s"HyperledgerException thrown by ${exp.transactionId}",
            exp.innerException.getMessage
          )
        case exp: NetworkExceptionTrait =>
          UC4Exception.InternalServerError(
            s"NetworkException with ${exp.toString}",
            exp.innerException.getMessage
          )
        case transactionEx: TransactionExceptionTrait =>
          try {
            val json = Json.parse(transactionEx.payload)
            try {
              errorMatching(json.as[UC4Error])
            }
            catch {
              case _: JsResultException =>
                UC4Exception.InternalServerError(
                  "Unknown UC4Error",
                  s"Hyperledger uses a new ErrorType ${json("type")}"
                )
            }
          }
          catch {
            case _: Throwable =>
              UC4Exception.InternalServerError(
                "Broken TransactionException",
                s"""id:      ${transactionEx.transactionId}
                   |payload: ${transactionEx.payload}
                   |""".stripMargin
              )
          }
        case exp: Throwable =>
          UC4Exception.InternalServerError("Unknown Internal Error", exp.getMessage)
      }
    }
  }

  private def errorMatching(uc4Error: UC4Error): UC4Exception = {
    uc4Error match {
      case genericError: GenericError => genericError.`type` match {
        case ErrorType.HLUnknownTransactionId => new UC4Exception(500, genericError)
        case ErrorType.HLNotFound => new UC4Exception(404, genericError)
        case ErrorType.HLConflict => new UC4Exception(409, genericError)
        case ErrorType.HLUnprocessableLedgerState => new UC4Exception(500, genericError)
        case _ => UC4Exception.InternalServerError("Unknown GenericError", s"Hyperledger uses a new ErrorType ${genericError.`type`}")
      }
      case detailedError: DetailedError => detailedError.`type` match {
        case ErrorType.HLUnprocessableEntity => new UC4Exception(422, detailedError)
        case _ => UC4Exception.InternalServerError("Unknown DetailedError", s"Hyperledger uses a new ErrorType ${detailedError.`type`}")
      }
      case transactionError: TransactionError => transactionError.`type` match {
        case ErrorType.HLInvalidTransactionCall => new UC4Exception(500, transactionError)
        case _ => UC4Exception.InternalServerError("Unknown TransactionError", s"Hyperledger uses a new ErrorType ${transactionError.`type`}")
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
