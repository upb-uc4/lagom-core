package de.upb.cs.uc4.hyperledger.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, UnsignedProposal, UnsignedTransaction }
import de.upb.cs.uc4.shared.client.UC4Service
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }

import java.util.{ Base64, Calendar }

trait UC4HyperledgerService extends UC4Service {

  val config: Config = null

  protected val jwtKey: String = config.getString("uc4.hyperledger.jwtKey")

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion]

  /** Creates an [[UnsignedProposal]] from a byte array
    * The val [[config]] needs to be overwritten on creation
    *
    * @param unsignedProposal the byte array
    * @return An [[UnsignedProposal]]
    */
  protected def createTimedUnsignedProposal(unsignedProposal: Array[Byte]): UnsignedProposal =
    UnsignedProposal(createTimedToken(unsignedProposal))

  /** Creates an [[UnsignedTransaction]] from a byte array
    * The val [[config]] needs to be overwritten on creation
    *
    * @param unsignedTransaction the byte array
    * @return An [[UnsignedTransaction]]
    */
  protected def createTimedUnsignedTransaction(unsignedTransaction: Array[Byte]): UnsignedTransaction =
    UnsignedTransaction(createTimedToken(unsignedTransaction))

  private def createTimedToken(bytes: Array[Byte]): String = {
    val now = Calendar.getInstance()
    now.add(Calendar.MINUTE, config.getInt("uc4.hyperledger.processingTime"))

    Jwts.builder()
      .setSubject("timed")
      .setExpiration(now.getTime)
      .claim("unsignedBytes", Base64.getEncoder.encodeToString(bytes))
      .signWith(SignatureAlgorithm.HS256, jwtKey)
      .compact()
  }

  override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/version/hyperledger", getHlfVersions _),
        restCall(Method.OPTIONS, pathPrefix + "/version/hyperledger", allowVersionNumber _)
      )
  }
}
