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

import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.event.Logging
import akka.serialization._
import com.esotericsoftware.kryo
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.esotericsoftware.kryo.util._
import com.esotericsoftware.minlog.{Log => MiniLog}
import com.typesafe.config.Config
import io.altoo.akka.serialization.kryo.serializer.akka.ActorRefSerializer
import io.altoo.akka.serialization.kryo.serializer.scala.{ScalaKryo, _}
import org.objenesis.strategy.StdInstantiatorStrategy

import scala.jdk.CollectionConverters._
import scala.util._


private[kryo] class KryoSerializationSettings(val config: Config) {
  val serializerType: String = config.getString("akka-kryo-serialization.type")

  val bufferSize: Int = config.getInt("akka-kryo-serialization.buffer-size")
  val maxBufferSize: Int = config.getInt("akka-kryo-serialization.max-buffer-size")

  // Each entry should be: FQCN -> integer id
  val classNameMappings: Map[String, String] = configToMap(config.getConfig("akka-kryo-serialization.mappings"))
  val classNames: java.util.List[String] = config.getStringList("akka-kryo-serialization.classes")

  // Strategy: default, explicit, incremental, automatic
  val idStrategy: String = config.getString("akka-kryo-serialization.id-strategy")
  val implicitRegistrationLogging: Boolean = config.getBoolean("akka-kryo-serialization.implicit-registration-logging")

  val kryoTrace: Boolean = config.getBoolean("akka-kryo-serialization.kryo-trace")
  val kryoReferenceMap: Boolean = config.getBoolean("akka-kryo-serialization.kryo-reference-map")
  val kryoDefaultSerializer: String = config.getString("akka-kryo-serialization.kryo-default-serializer")
  val kryoCustomSerializerInit: String = Try(config.getString("akka-kryo-serialization.kryo-custom-serializer-init")).getOrElse(null)

  val useManifests: Boolean = config.getBoolean("akka-kryo-serialization.use-manifests")

  val useUnsafe: Boolean = config.getBoolean("akka-kryo-serialization.use-unsafe")

  val aesKeyClass: String = Try(config.getString("akka-kryo-serialization.encryption.aes.custom-key-class")).getOrElse(null)
  val aesKey: String = Try(config.getString(s"akka-kryo-serialization.encryption.aes.key")).getOrElse("ThisIsASecretKey")
  val aesMode: String = Try(config.getString(s"akka-kryo-serialization.encryption.aes.mode")).getOrElse("AES/CBC/PKCS5Padding")
  val aesIvLength: Int = Try(config.getInt(s"akka-kryo-serialization.encryption.aes.IV-length")).getOrElse(16)

  val postSerTransformations: String = Try(config.getString("akka-kryo-serialization.post-serialization-transformations")).getOrElse("off")

  val customQueueBuilder: String = Try(config.getString("akka-kryo-serialization.custom-queue-builder")).getOrElse(null)

  val resolveSubclasses: Boolean = config.getBoolean("akka-kryo-serialization.resolve-subclasses")


  private def configToMap(cfg: Config): Map[String, String] =
    cfg.root.unwrapped.asScala.toMap.map { case (k, v) => (k, v.toString) }
}

class KryoSerializer(val system: ExtendedActorSystem) extends Serializer {

  private val log = Logging(system, getClass.getName)
  private val settings = new KryoSerializationSettings(system.settings.config)

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
    log.debug("Got default serializer class: {}", settings.kryoDefaultSerializer)
    log.debug("Got custom serializer init class: {}", settings.kryoCustomSerializerInit)
    log.debug("Got custom aes key class: {}", settings.aesKeyClass)
    log.debug("Got transformations: {}", settings.postSerTransformations)
    log.debug("Got queue builder: {}", settings.customQueueBuilder)
    log.debug("Got resolveSubclasses: {}", settings.resolveSubclasses)
  }

  private val customSerializerInitClass: Some[Class[_ <: AnyRef]] =
    if (settings.kryoCustomSerializerInit == null)
      null
    else
      system.dynamicAccess.getClassFor[AnyRef](settings.kryoCustomSerializerInit) match {
        case Success(clazz) => Some(clazz)
        case Failure(e) =>
          log.error("Class could not be loaded and/or registered: {} ", settings.kryoCustomSerializerInit)
          throw e
      }

  private val customizerInstance = Try(customSerializerInitClass.map(_.getDeclaredConstructor().newInstance()))
  private val customizerMethod = Try(customSerializerInitClass.map(_.getMethod("customize", classOf[Kryo])))

  private val defaultSerializerClass =
    system.dynamicAccess.getClassFor[kryo.Serializer[_]](settings.kryoDefaultSerializer) match {
      case Success(clazz) => clazz
      case Failure(e) =>
        log.error("Class could not be loaded and/or registered: {}", settings.kryoDefaultSerializer)
        throw e
    }

  private val customAESKeyClass: Some[Class[_ <: AnyRef]] =
    if (settings.aesKeyClass == null) null else
      system.dynamicAccess.getClassFor[AnyRef](settings.aesKeyClass) match {
        case Success(clazz) => Some(clazz)
        case Failure(e) =>
          log.error("Class could not be loaded {} ", settings.aesKeyClass)
          throw e
      }

  private val customAESKeyInstance = Try(customAESKeyClass.map(_.getDeclaredConstructor().newInstance()))
  private val aesKeyMethod = Try(customAESKeyClass.map(_.getMethod("kryoAESKey")))
  private[kryo] val aesKey: String = Try(aesKeyMethod.get.get.invoke(customAESKeyInstance.get.get).asInstanceOf[String]).getOrElse(settings.aesKey)

  protected val transform: String => Transformer = {
    case "lz4" => new LZ4KryoCompressor
    case "deflate" => new ZipKryoCompressor
    case "aes" => new KryoCryptographer(aesKey, settings.aesMode, settings.aesIvLength)
    case "off" => new NoKryoTransformer
    case x => throw new Exception(s"Could not recognise the transformer: [$x]")
  }

  private val kryoTransformer = new KryoTransformer(settings.postSerTransformations.split(",").toList.map(transform))

  private val queueBuilder: QueueBuilder =
    if (settings.customQueueBuilder == null) null
    else system.dynamicAccess.getClassFor[AnyRef](settings.customQueueBuilder) match {
      case Success(clazz) => clazz.getDeclaredConstructor().newInstance().asInstanceOf[QueueBuilder]
      case Failure(e) =>
        log.error("Class could not be loaded: {} ", settings.customQueueBuilder)
        throw e
    }

  // serializer pool to delegate actual serialization
  private val serializerPool = new SerializerPool(queueBuilder, () => new KryoSerializerBackend(getKryo(settings.idStrategy, settings.serializerType), settings.bufferSize, settings.maxBufferSize, settings.useManifests, settings.useUnsafe)(log))

  // this is whether "fromBinary" requires a "clazz" or not
  override def includeManifest: Boolean = settings.useManifests

  // a unique identifier for this Serializer
  override def identifier = 123454323

  // Delegate to a serializer backend
  override def toBinary(obj: AnyRef): Array[Byte] = {
    val ser = serializerPool.fetch()
    try
      kryoTransformer.toBinary(ser.toBinary(obj))
    finally
      serializerPool.release(ser)
  }

  // delegate to a serializer backend
  override def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    val ser = serializerPool.fetch()
    try
      ser.fromBinary(kryoTransformer.fromBinary(bytes), clazz)
    finally
      serializerPool.release(ser)
  }


  private def getKryo(strategy: String, serializerType: String): Kryo = {
    val referenceResolver = if (settings.kryoReferenceMap) new MapReferenceResolver() else new ListReferenceResolver()
    val classResolver =
      if (settings.idStrategy == "incremental") new KryoClassResolver(settings.implicitRegistrationLogging)
      else if (settings.resolveSubclasses) new SubclassResolver()
      else new DefaultClassResolver()
    val kryo = new ScalaKryo(classResolver, referenceResolver, new DefaultStreamFactory())
    kryo.setClassLoader(system.dynamicAccess.classLoader)
    // support deserialization of classes without no-arg constructors
    val instStrategy = kryo.getInstantiatorStrategy.asInstanceOf[Kryo.DefaultInstantiatorStrategy]
    instStrategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy())
    kryo.setInstantiatorStrategy(instStrategy)

    // setting default serializer
    kryo.setDefaultSerializer(defaultSerializerClass)

    // Support serialization of some standard or often used Scala classes
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    system.dynamicAccess.getClassFor[AnyRef]("scala.Enumeration$Val") match {
      case Success(clazz) => kryo.register(clazz)
      case Failure(e) =>
        log.error("Class could not be loaded and/or registered: {} ", "scala.Enumeration$Val")
        throw e
    }
    kryo.register(classOf[scala.Enumeration#Value])

    // identity preserving serializers for Unit and BoxedUnit
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])

    // mutable maps
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.Map[_, _]], classOf[ScalaMutableMapSerializer])

    // immutable maps - specialized by mutable, immutable and sortable
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.SortedMap[_, _]], classOf[ScalaSortedMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Map[_, _]], classOf[ScalaImmutableMapSerializer])

    // Sets - specialized by mutability and sortability
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.BitSet], classOf[FieldSerializer[scala.collection.immutable.BitSet]])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.SortedSet[_]], classOf[ScalaImmutableSortedSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[_]], classOf[ScalaImmutableSetSerializer])

    kryo.addDefaultSerializer(classOf[scala.collection.mutable.BitSet], classOf[FieldSerializer[scala.collection.mutable.BitSet]])
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.SortedSet[_]], classOf[ScalaMutableSortedSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.Set[_]], classOf[ScalaMutableSetSerializer])

    // Map/Set Factories
    ScalaVersionSerializers.mapAndSet(kryo)

    kryo.addDefaultSerializer(classOf[akka.util.ByteString], classOf[AkkaByteStringSerializer])
    ScalaVersionSerializers.iterable(kryo)
    kryo.addDefaultSerializer(classOf[ActorRef], new ActorRefSerializer(system))

    if (settings.kryoTrace)
      MiniLog.TRACE()

    kryo.setRegistrationRequired(strategy == "explicit")

    if (strategy != "default") {

      // register the class mappings and classes
      for ((fqcn: String, idNum: String) <- settings.classNameMappings) {
        val id = idNum.toInt
        // Load class
        system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
          case Success(clazz) => kryo.register(clazz, id)
          case Failure(e) =>
            log.error("Class could not be loaded and/or registered: {} ", fqcn)
            throw e
        }
      }

      for (classname <- settings.classNames.asScala) {
        // Load class
        system.dynamicAccess.getClassFor[AnyRef](classname) match {
          case Success(clazz) => kryo.register(clazz)
          case Failure(e) =>
            log.warning("Class could not be loaded and/or registered: {} ", classname)
          /* throw e */
        }
      }
    }

    serializerType match {
      case "graph" => kryo.setReferences(true)
      case _ => kryo.setReferences(false)
    }

    Try(customizerMethod.get.get.invoke(customizerInstance.get.get, kryo))

    classResolver match {
      // Now that we're done with registration, turn on the SubclassResolver:
      case resolver: SubclassResolver => resolver.enable()
      case _ =>
    }

    kryo
  }
}