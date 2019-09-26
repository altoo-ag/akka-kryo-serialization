/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */

package com.romix.akka.serialization.kryo

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.event.Logging
import com.typesafe.config.Config

import scala.jdk.CollectionConverters._
import scala.util.Try

object KryoSerialization {

  class Settings(val config: Config) {

    // type can be: graph, simple
    val serializerType: String = config.getString("akka.actor.kryo.type")

    val bufferSize: Int = config.getInt("akka.actor.kryo.buffer-size")
    val maxBufferSize: Int = config.getInt("akka.actor.kryo.max-buffer-size")

    // Each entry should be: FQCN -> integer id
    val classNameMappings: Map[String, String] = configToMap(config.getConfig("akka.actor.kryo.mappings"))
    val classNames: java.util.List[String] = config.getStringList("akka.actor.kryo.classes")

    // Strategy: default, explicit, incremental, automatic
    val idStrategy: String = config.getString("akka.actor.kryo.idstrategy")
    val implicitRegistrationLogging: Boolean = config.getBoolean("akka.actor.kryo.implicit-registration-logging")

    val kryoTrace: Boolean = config.getBoolean("akka.actor.kryo.kryo-trace")
    val kryoReferenceMap: Boolean = config.getBoolean("akka.actor.kryo.kryo-reference-map")
    val kryoDefaultSerializer: String = Try(config.getString("akka.actor.kryo.kryo-default-serializer")).getOrElse("com.esotericsoftware.kryo.serializers.FieldSerializer")
    val kryoCustomSerializerInit: String = Try(config.getString("akka.actor.kryo.kryo-custom-serializer-init")).getOrElse(null)

    val useManifests: Boolean = config.getBoolean("akka.actor.kryo.use-manifests")

    val useUnsafe: Boolean = config.getBoolean("akka.actor.kryo.use-unsafe")

    val aesKeyClass: String = Try(config.getString("akka.actor.kryo.encryption.aes.custom-key-class")).getOrElse(null)
    val aesKey: String = Try(config.getString(s"akka.actor.kryo.encryption.aes.key")).getOrElse("ThisIsASecretKey")
    val aesMode: String = Try(config.getString(s"akka.actor.kryo.encryption.aes.mode")).getOrElse("AES/CBC/PKCS5Padding")
    val aesIvLength: Int = Try(config.getInt(s"akka.actor.kryo.encryption.aes.IV-length")).getOrElse(16)

    val postSerTransformations: String = Try(config.getString("akka.actor.kryo.post-serialization-transformations")).getOrElse("off")

    val customQueueBuilder: String = Try(config.getString("akka.actor.kryo.custom-queue-builder")).getOrElse(null)

    val resolveSubclasses: Boolean = config.getBoolean("akka.actor.kryo.resolve-subclasses")


    private def configToMap(cfg: Config): Map[String, String] =
      cfg.root.unwrapped.asScala.toMap.map { case (k, v) => (k, v.toString) }
  }
}

class KryoSerialization(val system: ExtendedActorSystem) extends Extension {
  import KryoSerialization._

  val settings = new Settings(system.settings.config)
  val log = Logging(system, getClass.getName)

}

object KryoSerializationExtension extends ExtensionId[KryoSerialization] with ExtensionIdProvider {
  override def get(system: ActorSystem): KryoSerialization = super.get(system)
  override def lookup: KryoSerializationExtension.type = KryoSerializationExtension
  override def createExtension(system: ExtendedActorSystem): KryoSerialization = new KryoSerialization(system)
}

trait KryoCrypto {
  def kryoAESKey: String
}
