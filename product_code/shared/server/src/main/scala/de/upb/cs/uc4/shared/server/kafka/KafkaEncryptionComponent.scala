package de.upb.cs.uc4.shared.server.kafka

import com.lightbend.lagom.scaladsl.api.LagomConfigComponent
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.shared.server.SecretsUtility
import javax.crypto.SecretKey

/** Used to allow services access the [[de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionUtility]]
  */
trait KafkaEncryptionComponent extends LagomConfigComponent {

  protected val secretKey: SecretKey =
    SecretsUtility.deriveKey(config.getString("secrets.master"), config.getString("secrets.salts.kafka"))

  val kafkaEncryptionUtility: KafkaEncryptionUtility = wire[KafkaEncryptionUtility]
}
