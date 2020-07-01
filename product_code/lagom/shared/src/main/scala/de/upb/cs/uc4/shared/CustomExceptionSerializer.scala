package de.upb.cs.uc4.shared

import java.io.{CharArrayWriter, PrintWriter}

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.{DefaultExceptionSerializer, RawExceptionMessage}
import com.lightbend.lagom.scaladsl.api.transport.{ExceptionMessage, MessageProtocol, TransportErrorCode, TransportException}
import de.upb.cs.uc4.shared.messages.PossibleErrorResponse
import play.api.libs.json.{JsArray, Json}
import play.api.{Environment, Mode}

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
      case per :PossibleErrorResponse =>

        var arr : JsArray = Json.arr()
        for (error <- per.errors){
          arr :+= Json.obj(
            "name" -> error.name,
            "reason" -> error.reason
          )
        }
        ByteString.fromString(Json.stringify(Json.obj(
        "type" -> per.`type`,
        "title" -> per.title,
        "errors" -> arr
      )))

      case message: ExceptionMessage => ByteString.fromString(Json.stringify(Json.obj(
        "name" -> message.name,
        "detail" -> message.detail
      ))) //If it is not one of our custom types of Exceptions, it is one of the default exception

      case _ => ByteString("")
    }

    RawExceptionMessage(errorCode, MessageProtocol(Some("application/json"), None, None), messageBytes)
  }
}