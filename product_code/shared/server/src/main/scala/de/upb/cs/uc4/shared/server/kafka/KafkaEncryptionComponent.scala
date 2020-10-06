package de.upb.cs.uc4.shared.server.kafka

import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.softwaremill.macwire.wire
import javax.crypto.spec.{ PBEKeySpec, SecretKeySpec }
import javax.crypto.{ SecretKey, SecretKeyFactory }

trait KafkaEncryptionComponent extends LagomConfigComponent {

  protected val secretKey: SecretKey = {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val spec = new PBEKeySpec(
      config.getString("master.secret").toCharArray,
      config.getString("master.salt").getBytes,
      65536,
      256
    )
    new SecretKeySpec(factory.generateSecret(spec).getEncoded, "AES")
  }

  val kafkaEncryptionUtility: KafkaEncryptionUtility = wire[KafkaEncryptionUtility]
}
