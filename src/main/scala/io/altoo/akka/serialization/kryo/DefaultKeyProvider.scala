package io.altoo.akka.serialization.kryo

import com.typesafe.config.Config
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Default encryption key provider that can be extended to provide encryption key differently.
 */
class DefaultKeyProvider {

  /**
   * @param config The config scope of the serializer
   */
  def aesKey(config: Config): Array[Byte] = {
    val password = config.getString("encryption.aes.password")
    val salt = config.getString("encryption.aes.salt")
    deriveKey(password, salt)
  }

  protected final def deriveKey(password: String, salt: String): Array[Byte] = {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = new PBEKeySpec(password.toCharArray, salt.getBytes("UTF-8"), 65536, 256)
    factory.generateSecret(spec).getEncoded
  }
}