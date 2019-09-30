package io.altoo.akka.serialization.kryo

import com.typesafe.config.Config

/**
 * Default encryption key provider that can be extended to provide encryption key differently.
 */
class DefaultKeyProvider {

  /**
   * @param config The config scope of the serializer
   */
  def aesKey(config: Config): String = config.getString("encryption.aes.key")
}
