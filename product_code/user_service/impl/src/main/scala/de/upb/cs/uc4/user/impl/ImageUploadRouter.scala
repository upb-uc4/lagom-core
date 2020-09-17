package de.upb.cs.uc4.user.impl

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import de.upb.cs.uc4.shared.client.exceptions.{ CustomException, ErrorType, GenericError }
import play.api.mvc.{ DefaultActionBuilder, PlayBodyParsers, Result, Results }
import play.api.routing.Router
import play.api.routing.sird._

import scala.concurrent.{ ExecutionContext, Future }

class ImageUploadRouter(action: DefaultActionBuilder, parser: PlayBodyParsers, userApplication: UserApplication) {
  private lazy val userService = userApplication.lagomServer.serviceBinding.service.asInstanceOf[UserServiceImpl]

  private implicit val executionContext: ExecutionContext = userApplication.executionContext

  val router: Router = Router.from {
    case PUT(p"/user-management/users/$username<[^/]+>/image") =>
      action.async(parser.raw) { request =>

        val serviceRequest = RequestHeader.Default.withHeaders(request.headers.headers)

        val filePath: String = request.body.asFile.getAbsolutePath

        try {
          userService.setImage(username).invokeWithHeaders(serviceRequest, filePath).map {
            case (header, _) => Results.Created.withHeaders(header.headers.toSeq: _*)
          }.recover(handleException)
        }
        catch handleException.andThen(result => Future.successful(result))
      }
  }

  private def handleException: PartialFunction[Throwable, Result] = {
    case customException: CustomException =>
      new Results.Status(customException.getErrorCode.http)(customException.getPossibleErrorResponse)
    case _: Exception =>
      Results.InternalServerError(GenericError(ErrorType.InternalServer))
  }
}
