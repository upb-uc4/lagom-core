package de.upb.cs.uc4.shared.client.exceptions

import java.io.{CharArrayWriter, PrintWriter}

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.{DefaultExceptionSerializer, RawExceptionMessage}
import com.lightbend.lagom.scaladsl.api.transport.{ExceptionMessage, MessageProtocol, TransportErrorCode, TransportException}
import play.api.libs.json.{JsArray, JsError, JsSuccess, Json}
import play.api.{Environment, Mode}

import scala.util.control.NonFatal

class CustomExceptionSerializer(environment: Environment) extends DefaultExceptionSerializer(environment) {

  override def serialize(exception: Throwable, accept: Seq[MessageProtocol]): RawExceptionMessage = {
    val (errorCode, message) = exception match {
      case ce: CustomException => (ce.getErrorCode, ce.getPossibleErrorResponse)
      case te: TransportException =>
        (te.errorCode, te.exceptionMessage)
      case e if environment.mode == Mode.Prod =>
        // By default, don't give out information about generic exceptions.
        (TransportErrorCode.InternalServerError, new ExceptionMessage("Exception", ""))
      case e =>
        // Ok to give out exception information in dev and test
        val writer = new CharArrayWriter
        e.printStackTrace(new PrintWriter(writer))
        val detail = writer.toString
        (TransportErrorCode.InternalServerError, new ExceptionMessage(s"${exception.getClass.getName}: ${exception.getMessage}", detail))
    }

    val messageBytes = message match {
      case der: DetailedError =>

        var arr: JsArray = Json.arr()
        for (error <- der.invalidParams) {
          arr :+= Json.obj(
            "name" -> error.name,
            "reason" -> error.reason
          )
        }
        ByteString.fromString(Json.stringify(Json.obj(
          "type" -> der.`type`,
          "title" -> der.title,
          "invalidParams" -> arr
        )))

      case ter: TransactionError =>
        ByteString.fromString(Json.stringify(Json.obj(
          "type" -> ter.`type`,
          "title" -> ter.title,
          "transactionId" -> ter.transactionId
        )))

      case ger: GenericError =>
        ByteString.fromString(Json.stringify(Json.obj(
          "type" -> ger.`type`,
          "title" -> ger.title
        )))

      case message: ExceptionMessage => ByteString.fromString(Json.stringify(Json.obj(
        "name" -> message.name,
        "detail" -> message.detail
      ))) //If it is not one of our custom types of Exceptions, it is one of the default exception

      case _ => ByteString("")
    }

    RawExceptionMessage(errorCode, MessageProtocol(Some("application/json"), None, None), messageBytes)
  }

  override def deserialize(message: RawExceptionMessage): Throwable = {
    val messageJson =
      try {
        Json.parse(message.message.iterator.asInputStream)
      }
      catch {
        case NonFatal(e) =>
          Json.obj()
      }

    //Check if the raw json contains the fields needed for a CustomException (type, title, invalidParams). If so, use our deserializer, if not, use default
    if (messageJson.toString().contains("type") && messageJson.toString().contains("title")) {
      //We have a CustomError, this part is different from the default serializer
      //Match for Error Subtype
      messageJson.toString() match {
        //DetailedError
        case jsonString: String if jsonString.contains("invalidParams") =>
          val jsonParseResult = for {
            eType <- (messageJson \ "type").validate[String]
            title <- (messageJson \ "title").validate[String]
            invalidParams <- (messageJson \ "invalidParams").validate[Seq[SimpleError]]

          } yield new DetailedError(eType, title, invalidParams)
          val detailedError = jsonParseResult match {
            case JsSuccess(m, _) => m
            case JsError(_)      => GenericError("deserialization exception")
          }
          fromCodeAndMessageCustom(message.errorCode, detailedError)

        //TransactionError
        case jsonString: String if jsonString.contains("transactionId") =>
          val jsonParseResult = for {
            eType <- (messageJson \ "type").validate[String]
            title <- (messageJson \ "title").validate[String]
            transactionId <- (messageJson \ "transactionId").validate[String]

          } yield new TransactionError(eType, title, transactionId)
          val transactionError = jsonParseResult match {
            case JsSuccess(m, _) => m
            case JsError(_)      => GenericError("deserialization exception")
          }
          fromCodeAndMessageCustom(message.errorCode, transactionError)

        //GenericError
        case _ =>
          val jsonParseResult = for {
            eType <- (messageJson \ "type").validate[String]
            title <- (messageJson \ "title").validate[String]

          } yield new GenericError(eType, title)
          val genericError = jsonParseResult match {
            case JsSuccess(m, _) => m
            case JsError(_)      => GenericError("deserialization exception")
          }
          fromCodeAndMessageCustom(message.errorCode, genericError)
      }
    }
    else {
      //Default serializer for Exceptions with fields "name" and "detail"
      val jsonParseResult = for {
        name <- (messageJson \ "name").validate[String]
        detail <- (messageJson \ "detail").validate[String]
      } yield new ExceptionMessage(name, detail)
      val exceptionMessage = jsonParseResult match {
        case JsSuccess(m, _) => m
        case JsError(_)      => new ExceptionMessage("deserialization exception", message.message.utf8String)
      }
      fromCodeAndMessage(message.errorCode, exceptionMessage)
    }

  }

  /** Override this if you wish to deserialize your own custom Exceptions.
    *
    * The default implementation delegates to [[TransportException.fromCodeAndMessage()]], which will return a best match
    * Lagom built-in exception.
    *
    * @param transportErrorCode The transport error code.
    * @param exceptionMessage The exception message.
    * @return The exception.
    */
  override def fromCodeAndMessage(
      transportErrorCode: TransportErrorCode,
      exceptionMessage: ExceptionMessage
  ): Throwable = {
    TransportException.fromCodeAndMessage(transportErrorCode, exceptionMessage)
  }
  /** Used to deserialize our CustomExceptions.
    *
    * @param transportErrorCode The transport error code.
    * @param customError The detailed Error.
    * @return The exception.
    */
  def fromCodeAndMessageCustom(
      transportErrorCode: TransportErrorCode,
      customError: CustomError
  ): Throwable = {
    new CustomException(transportErrorCode, customError)
  }
}

