package de.upb.cs.uc4.shared.server.kafka

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.JsonUtility._
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionUtility.{ GCM_IV_LENGTH, secureRandom }
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.{ AEADBadTagException, Cipher, SecretKey }
import play.api.libs.json.{ Format, __ }

/** Provides methods for encryption, decryption and authentication of objects which shall be send through Kafka
  *
  * @param secretKey symmetric key for authenticated encryption
  */
class KafkaEncryptionUtility(secretKey: SecretKey) {

  /** Given an objectToEncrypt it will be encrypted and authenticated with AES-GCM. The canonical name of the class
    * type is used as associated data, therefore encrypted payload and plaintext class type are authenticated.
    *
    * @param objectToEncrypt Json serializable object which shall be send encrypted and authenticated through Kafka
    * @param format implicit to allow toJson conversion
    * @tparam Type of the objectToEncrypt
    * @throws Throwable javax.crypto exceptions, IO exceptions, Json serialization exception if objectToEncrypt is invalid e.g. null
    * @return [[de.upb.cs.uc4.shared.client.kafka.EncryptionContainer]] wrapping the objectToEncrypt
    */

  def encrypt[Type](objectToEncrypt: Type)(implicit format: Format[Type]): EncryptionContainer = {
    val plaintext: String = objectToEncrypt.toJson
    val associatedData = objectToEncrypt.getClass.getCanonicalName

    val iv = new Array[Byte](GCM_IV_LENGTH) //NEVER REUSE THIS IV WITH SAME KEY
    secureRandom.nextBytes(iv)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val parameterSpec = new GCMParameterSpec(128, iv) //128 bit auth tag length
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

    cipher.updateAAD(associatedData.getBytes)

    val cipherText: Array[Byte] = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8))

    val byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length)
    byteBuffer.put(iv)
    byteBuffer.put(cipherText)
    EncryptionContainer(associatedData, byteBuffer.array)
  }

  /** Given an valid [[de.upb.cs.uc4.shared.client.kafka.EncryptionContainer]] it will be unpacked into unencrypted
    * object.
    *
    * @param container which contains encrypted object received through kafka
    * @param format implicit to allow fromJson conversion
    * @tparam Type of the encrypted object, used for deserialization
    * @throws Throwable javax.crypto exceptions, IO exceptions, Json deserialization exception if objectToEncrypt is invalid e.g. null, especially AEADBadTagException if auth tag has been altered
    * @return unencrypted object of provided Type
    */
  def decrypt[Type](container: EncryptionContainer)(implicit format: Format[Type]): Type = {
    val cipherMessage = container.data

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    //use first 12 bytes for iv
    val gcmIv = new GCMParameterSpec(128, cipherMessage, 0, GCM_IV_LENGTH)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmIv)

    cipher.updateAAD(container.classType.getBytes)
    //use everything from 12 bytes on as ciphertext
    val plainText = cipher.doFinal(cipherMessage, GCM_IV_LENGTH, cipherMessage.length - GCM_IV_LENGTH)
    new String(plainText, StandardCharsets.UTF_8).fromJson[Type]
  }
}

object KafkaEncryptionUtility {
  /** For GCM a 12 byte (not 16!) random (or counter) byte-array is recommend by NIST,
    * because it’s faster and more secure.
    */
  protected val GCM_IV_LENGTH = 12
  protected val secureRandom: SecureRandom = new SecureRandom()
}
