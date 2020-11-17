package de.upb.cs.uc4.shared.server

import java.util.Calendar

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }

/** Utility trait containing useful methods for testing service implementations */
trait UC4SpecUtils {

  /** Creates an authentication header for the username with the given role, signs it with the signing key and adds it
    * to the request header.
    * Providing a username that contains the name of an authentication role in lowercase (e.g. student0) will
    * automatically use the appropriate authentication role (e.g. for student0 Student).
    * If the name does not contain any name of a role, the role parameter of this method will be used instead.
    *
    * @param username of the user to create an authentication header for
    * @param role the role of the user to create an authentication header for
    * @param signingKey the key used to sign the JWTs
    */
  def addAuthorizationHeader(
      username: String = "admin",
      role: AuthenticationRole = AuthenticationRole.Admin,
      signingKey: String = "changeme"
  ): RequestHeader => RequestHeader =
    header => header.withHeader("Cookie", createLoginToken(username, role, signingKey))

  /** Creates a login token with the given username and authentication role, signed with the given key. */
  def createLoginToken(
      username: String = "admin",
      authRole: AuthenticationRole = AuthenticationRole.Admin,
      signingKey: String = "changeme"
  ): String = {

    /* for convenience, using the appropriate role name in the username automatically assigns the role
       if this is not given, the parameter authRole will be used instead */
    val role = username match {
      case _ if username.contains("student") => AuthenticationRole.Student
      case _ if username.contains("lecturer") => AuthenticationRole.Lecturer
      case _ if username.contains("admin") => AuthenticationRole.Admin
      case _ => authRole
    }

    val time = Calendar.getInstance()
    time.add(Calendar.DATE, 1)

    val token =
      Jwts.builder()
        .setSubject("login")
        .setExpiration(time.getTime)
        .claim("username", username)
        .claim("authenticationRole", role.toString)
        .signWith(SignatureAlgorithm.HS256, signingKey)
        .compact()

    s"login=$token"
  }

}
