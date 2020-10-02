package de.upb.cs.uc4.user.impl

import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import de.upb.cs.uc4.shared.client.exceptions.{ UC4Exception, ErrorType, GenericError }
import play.api.mvc.{ DefaultActionBuilder, PlayBodyParsers, Result, Results }
import play.api.routing.Router
import play.api.routing.sird._

import scala.concurrent.{ ExecutionContext, Future }

class ImageUploadRouter(action: DefaultActionBuilder, parser: PlayBodyParsers, userApplication: UserApplication) {
  private lazy val userService = userApplication.lagomServer.serviceBinding.service.asInstanceOf[UserServiceImpl]
  private lazy val maxSize = userApplication.config.getInt("uc4.image.maxSize")

  private implicit val executionContext: ExecutionContext = userApplication.executionContext
  private implicit val materializer: Materializer = userApplication.materializer

  val router: Router = Router.from {
    case PUT(p"/user-management/users/$username<[^/]+>/image") =>
      action.async(parser.maxLength(maxSize, parser.raw)) { request =>

        request.body match {
          case Left(_) =>
            Future.successful(Results.EntityTooLarge(GenericError(ErrorType.EntityTooLarge)))
          case Right(buffer) =>
            val serviceRequest = RequestHeader.Default.withHeaders(request.headers.headers)

            try {
              userService.setImage(username).invokeWithHeaders(serviceRequest, buffer.asBytes(maxSize).get.toArray).map {
                case (header, _) => Results.Ok.withHeaders(header.headers.toSeq: _*)
              }.recover(handleException)
            }
            catch handleException.andThen(result => Future.successful(result))
        }
      }
  }

  private def handleException: PartialFunction[Throwable, Result] = {
    case customException: UC4Exception =>
      new Results.Status(customException.errorCode.http)(customException.possibleErrorResponse)
    case _: Exception =>
      Results.InternalServerError(GenericError(ErrorType.InternalServer))
  }
}
