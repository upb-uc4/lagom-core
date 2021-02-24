package de.upb.cs.uc4.hyperledger.api

import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import java.util.Base64

object JWTClaimParser {

  def readClaim(claim: String, token: String): String = {
    token match {
      case s"$_.$body.$_" =>
        val json = new String(Base64.getDecoder.decode(body), StandardCharsets.UTF_8)

        Json.parse(json)(claim).asOpt[String] match {
          case Some(value) => value
          case None        => throw UC4Exception.InternalServerError("Unknown claim", s"The claim $claim does not exist")
        }
      case _ =>
        throw UC4Exception.InternalServerError("Malformed token", s"The token $token is not a JWT")
    }
  }
}
