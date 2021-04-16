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

package io.altoo.akka.serialization.kryo

import akka.actor.ExtendedActorSystem
import akka.event.Logging
import akka.serialization._
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util._
import com.esotericsoftware.minlog.{Log => MiniLog}
import com.typesafe.config.Config
import io.altoo.akka.serialization.kryo.serializer.scala.{ScalaKryo, _}
import org.objenesis.strategy.StdInstantiatorStrategy

import java.nio.ByteBuffer
import scala.jdk.CollectionConverters._
import scala.util._

private[kryo] class EncryptionSettings(val config: Config) {
  val keyProvider: String = config.getString("encryption.aes.key-provider")
  val aesMode: String = config.getString("encryption.aes.mode")
  val aesIvLength: Int = config.getInt("encryption.aes.iv-length")
}

private[kryo] class KryoSerializationSettings(val config: Config) {
  val serializerType: String = config.getString("type")

  val bufferSize: Int = config.getInt("buffer-size")
  val maxBufferSize: Int = config.getInt("max-buffer-size")

  // Each entry should be: FQCN -> integer id
  val classNameMappings: Map[String, String] = configToMap(config.getConfig("mappings"))
  val classNames: java.util.List[String] = config.getStringList("classes")

  // Strategy: default, explicit, incremental, automatic
  val idStrategy: String = config.getString("id-strategy")
  val implicitRegistrationLogging: Boolean = config.getBoolean("implicit-registration-logging")

  val kryoTrace: Boolean = config.getBoolean("kryo-trace")
  val kryoReferenceMap: Boolean = config.getBoolean("kryo-reference-map")
  val kryoInitializer: String = config.getString("kryo-initializer")

  val useManifests: Boolean = config.getBoolean("use-manifests")
  val useUnsafe: Boolean = config.getBoolean("use-unsafe")

  val encryptionSettings: Option[EncryptionSettings] = if (config.hasPath("encryption")) Some(new EncryptionSettings(config)) else None

  val postSerTransformations: String = config.getString("post-serialization-transformations")
  val queueBuilder: String = config.getString("queue-builder")
  val resolveSubclasses: Boolean = config.getBoolean("resolve-subclasses")


  private def configToMap(cfg: Config): Map[String, String] =
    cfg.root.unwrapped.asScala.toMap.map { case (k, v) => (k, v.toString) }
}

class KryoSerializer(val system: ExtendedActorSystem) extends Serializer with ByteBufferSerializer {

  protected def configKey: String = "akka-kryo-serialization"

  private val log = Logging(system, getClass.getName)
  private val config = system.settings.config.getConfig(configKey)
  private val settings = new KryoSerializationSettings(config)

  locally {
    log.debug("Got mappings: {}", settings.classNameMappings)
    log.debug("Got classnames for incremental strategy: {}", settings.classNames)
    log.debug("Got buffer-size: {}", settings.bufferSize)
    log.debug("Got max-buffer-size: {}", settings.maxBufferSize)
    log.debug("Got id strategy: {}", settings.idStrategy)
    log.debug("Got serializer type: {}", settings.serializerType)
    log.debug("Got implicit registration logging: {}", settings.implicitRegistrationLogging)
    log.debug("Got use manifests: {}", settings.useManifests)
    log.debug("Got use unsafe: {}", settings.useUnsafe)
    log.debug("Got serializer configuration class: {}", settings.kryoInitializer)
    log.debug("Got encryption settings: {}", settings.encryptionSettings)
    log.debug("Got transformations: {}", settings.postSerTransformations)
    log.debug("Got queue builder: {}", settings.queueBuilder)
    log.debug("Got resolveSubclasses: {}", settings.resolveSubclasses)
  }

  if (settings.kryoTrace)
    MiniLog.TRACE()

  private val kryoInitializerClass: Class[_ <: DefaultKryoInitializer] =
      system.dynamicAccess.getClassFor[AnyRef](settings.kryoInitializer) match {
        case Success(clazz) if classOf[DefaultKryoInitializer].isAssignableFrom(clazz) => clazz.asSubclass(classOf[DefaultKryoInitializer])
        case Success(clazz) =>
          log.error("Configured class {} does not extend DefaultKryoInitializer", clazz)
          throw new IllegalStateException(s"Configured class $clazz does not extend DefaultKryoInitializer")
        case Failure(e) =>
          log.error("Class could not be loaded: {} ", settings.kryoInitializer)
          throw e
      }

  private val aesKeyProviderClass: Option[Class[_ <: DefaultKeyProvider]] =
    settings.encryptionSettings.map(c =>
      system.dynamicAccess.getClassFor[AnyRef](c.keyProvider) match {
        case Success(clazz) if classOf[DefaultKeyProvider].isAssignableFrom(clazz) => clazz.asSubclass(classOf[DefaultKeyProvider])
        case Success(clazz) =>
          log.error("Configured class {} does not extend DefaultKeyProvider", clazz)
          throw new IllegalStateException(s"Configured class $clazz does not extend DefaultKryoInitializer")
        case Failure(e) =>
          log.error("Class could not be loaded: {} ", c.keyProvider)
          throw e
      }
    )

  protected val transform: String => Option[Transformer] = {
    case "off" => None
    case "lz4" => Some(new LZ4KryoCompressor)
    case "deflate" => Some(new ZipKryoCompressor)
    case "aes" => settings.encryptionSettings match {
      case Some(es) if es.aesMode.contains("GCM") =>
        Some(new KryoCryptographer(aesKeyProviderClass.get.getDeclaredConstructor().newInstance().aesKey(config), es.aesMode, es.aesIvLength))
      case Some(es) =>
        throw new Exception(s"Mode ${es.aesMode} is not supported for 'aes'")
      case None =>
        throw new Exception("Encryption transformation selected but encryption has not been configured")
    }
    case x => throw new Exception(s"Could not recognise the transformer: [$x]")
  }

  private val kryoTransformer = new KryoTransformer(settings.postSerTransformations.split(",").toList.flatMap(transform(_).toList))

  private val queueBuilderClass: Class[_ <: DefaultQueueBuilder] =
    system.dynamicAccess.getClassFor[AnyRef](settings.queueBuilder) match {
      case Success(clazz) if classOf[DefaultQueueBuilder].isAssignableFrom(clazz) => clazz.asSubclass(classOf[DefaultQueueBuilder])
      case Success(clazz) =>
        log.error("Configured class {} does not extend DefaultQueueBuilder", clazz)
        throw new IllegalStateException(s"Configured class $clazz does not extend DefaultQueueBuilder")
      case Failure(e) =>
        log.error("Class could not be loaded: {} ", settings.queueBuilder)
        throw e
    }

  // serializer pool to delegate actual serialization
  private val serializerPool = new SerializerPool(queueBuilderClass.getDeclaredConstructor().newInstance(), () => new KryoSerializerBackend(getKryo(settings.idStrategy, settings.serializerType), settings.bufferSize, settings.maxBufferSize, settings.useManifests, settings.useUnsafe)(system, log))

  // this is whether "fromBinary" requires a "clazz" or not
  override def includeManifest: Boolean = settings.useManifests

  // a unique identifier for this Serializer
  override def identifier = 123454323

  // Delegate to a serializer backend
  // Implements Serializer
  override def toBinary(obj: AnyRef): Array[Byte] = {
    val ser = serializerPool.fetch()
    try
      kryoTransformer.toBinary(ser.toBinary(obj))
    finally
      serializerPool.release(ser)
  }

  // Implements ByteBufferSerializer
  override def toBinary(obj: AnyRef, buf: ByteBuffer): Unit = {
    val ser = serializerPool.fetch()
    try {
      if (kryoTransformer.isIdentity)
        ser.toBinary(obj, buf)
      else
        kryoTransformer.toBinary(ser.toBinary(obj), buf)
    } finally
      serializerPool.release(ser)
  }

  // delegate to a serializer backend
  // Implements Serializer
  override def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    val ser = serializerPool.fetch()
    try
      ser.fromBinary(kryoTransformer.fromBinary(bytes), clazz)
    finally
      serializerPool.release(ser)
  }

  // Implements ByteBufferSerializer
  override def fromBinary(buf: ByteBuffer, manifest: String): AnyRef = {
    val ser = serializerPool.fetch()
    try {
      if (kryoTransformer.isIdentity)
        ser.fromBinary(buf, manifest)
      else
        ser.fromBinary(kryoTransformer.fromBinary(buf), system.dynamicAccess.getClassFor[AnyRef](manifest).toOption)
    } finally
      serializerPool.release(ser)
  }

  private def getKryo(strategy: String, serializerType: String): Kryo = {
    val referenceResolver = if (settings.kryoReferenceMap) new MapReferenceResolver() else new ListReferenceResolver()
    val classResolver =
      if (settings.idStrategy == "incremental") new KryoClassResolver(settings.implicitRegistrationLogging)
      else if (settings.resolveSubclasses) new SubclassResolver()
      else new DefaultClassResolver()
    val kryo = new ScalaKryo(classResolver, referenceResolver)
    kryo.setClassLoader(system.dynamicAccess.classLoader)
    // support deserialization of classes without no-arg constructors
    val instStrategy = kryo.getInstantiatorStrategy.asInstanceOf[DefaultInstantiatorStrategy]
    instStrategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy())
    kryo.setInstantiatorStrategy(instStrategy)
    kryo.setOptimizedGenerics(false) // causes issue serializing classes extending generic base classes

    serializerType match {
      case "graph" => kryo.setReferences(true)
      case "nograph" => kryo.setReferences(false)
      case o => throw new IllegalStateException("Unknown serializer type: " + o)
    }

    val initializer = kryoInitializerClass.getDeclaredConstructor().newInstance()

    // setting default serializer
    initializer.preInit(kryo, system)
    // akka byte string serializer must be registered before generic scala collection serializer
    initializer.initAkkaSerializer(kryo, system)
    initializer.initScalaSerializer(kryo, system)

    // if explicit we require all classes to be registered explicitely
    kryo.setRegistrationRequired(strategy == "explicit")

    // register configured class mappings and classes
    if (strategy != "default") {
      for ((fqcn: String, idNum: String) <- settings.classNameMappings) {
        val id = idNum.toInt
        system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
          case Success(clazz) => kryo.register(clazz, id)
          case Failure(e) =>
            log.error("Class could not be loaded and/or registered: {} ", fqcn)
            throw e
        }
      }

      for (classname <- settings.classNames.asScala) {
        system.dynamicAccess.getClassFor[AnyRef](classname) match {
          case Success(clazz) => kryo.register(clazz)
          case Failure(e) =>
            log.warning("Class could not be loaded and/or registered: {} ", classname)
            throw e
        }
      }
    }

    initializer.postInit(kryo, system)

    classResolver match {
      // Now that we're done with registration, turn on the SubclassResolver:
      case resolver: SubclassResolver => resolver.enable()
      case _ =>
    }

    kryo
  }
}
