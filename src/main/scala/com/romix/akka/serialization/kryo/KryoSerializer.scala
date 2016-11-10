/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */

package com.romix.akka.serialization.kryo

import java.security.SecureRandom
import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.{Deflater, Inflater}
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.event.Logging
import akka.serialization._
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output, UnsafeInput, UnsafeOutput}
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.esotericsoftware.kryo.util._
import com.esotericsoftware.minlog.{Log => MiniLog}
import com.romix.scala.serialization.kryo.{ScalaKryo, _}
import net.jpountz.lz4.LZ4Factory
import org.objenesis.strategy.StdInstantiatorStrategy

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuilder
import scala.util.{Failure, Success, Try}

trait Transformation {
  def toBinary(inputBuff: Array[Byte]): Array[Byte]
  def fromBinary(inputBuff: Array[Byte]): Array[Byte]
}

class NoKryoTransformer extends Transformation {
  def toBinary(inputBuff: Array[Byte]) = inputBuff
  def fromBinary(inputBuff: Array[Byte]) = inputBuff
}

class KryoTransformer(transformations: List[Transformation]) {
  private[this] val toPipeLine = transformations.map(x => x.toBinary _).reduceLeft(_ andThen _)
  private[this] val fromPipeLine = transformations.map(x => x.fromBinary _).reverse.reduceLeft(_ andThen _)

  def toBinary(inputBuff: Array[Byte]): Array[Byte] = {
    toPipeLine(inputBuff)
  }

  def fromBinary(inputBuff: Array[Byte]) = {
    fromPipeLine(inputBuff)
  }
}

class LZ4KryoCompressor extends Transformation {

  lazy val lz4factory = LZ4Factory.fastestInstance

  def toBinary(inputBuff: Array[Byte]): Array[Byte] = {
    val inputSize = inputBuff.length
    val lz4 = lz4factory.fastCompressor
    val maxOutputSize = lz4.maxCompressedLength(inputSize)
    val outputBuff = new Array[Byte](maxOutputSize + 4)
    val outputSize = lz4.compress(inputBuff, 0, inputSize, outputBuff, 4, maxOutputSize)

    // encode 32 bit length in the first bytes
    outputBuff(0) = (inputSize & 0xff).toByte
    outputBuff(1) = (inputSize >> 8 & 0xff).toByte
    outputBuff(2) = (inputSize >> 16 & 0xff).toByte
    outputBuff(3) = (inputSize >> 24 & 0xff).toByte
    outputBuff.take(outputSize + 4)
  }

  def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    // the first 4 bytes are the original size
    val size: Int = (inputBuff(0).asInstanceOf[Int] & 0xff) |
      (inputBuff(1).asInstanceOf[Int] & 0xff) << 8 |
      (inputBuff(2).asInstanceOf[Int] & 0xff) << 16 |
      (inputBuff(3).asInstanceOf[Int] & 0xff) << 24
    val lz4 = lz4factory.fastDecompressor()
    val outputBuff = new Array[Byte](size)
    lz4.decompress(inputBuff, 4, outputBuff, 0, size)
    outputBuff
  }
}

class ZipKryoCompressor extends Transformation {

  lazy val deflater = new Deflater(Deflater.BEST_SPEED)
  lazy val inflater = new Inflater()

  def toBinary(inputBuff: Array[Byte]): Array[Byte] = {
    val inputSize = inputBuff.length
    val outputBuff = new ArrayBuilder.ofByte
    outputBuff += (inputSize & 0xff).toByte
    outputBuff += (inputSize >> 8 & 0xff).toByte
    outputBuff += (inputSize >> 16 & 0xff).toByte
    outputBuff += (inputSize >> 24 & 0xff).toByte

    deflater.setInput(inputBuff)
    deflater.finish()
    val buff = new Array[Byte](4096)

    while (!deflater.finished) {
      val n = deflater.deflate(buff)
      outputBuff ++= buff.take(n)
    }
    deflater.reset()
    outputBuff.result
  }

  def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    val size: Int = (inputBuff(0).asInstanceOf[Int] & 0xff) |
      (inputBuff(1).asInstanceOf[Int] & 0xff) << 8 |
      (inputBuff(2).asInstanceOf[Int] & 0xff) << 16 |
      (inputBuff(3).asInstanceOf[Int] & 0xff) << 24
    val outputBuff = new Array[Byte](size)
    inflater.setInput(inputBuff, 4, inputBuff.length - 4)
    inflater.inflate(outputBuff)
    inflater.reset()
    outputBuff
  }
}

class KryoCryptographer(key: String, mode: String, ivLength: Int) extends Transformation {
  private[this] val sKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES")
  private[this] var iv: Array[Byte] = Array.fill[Byte](ivLength)(0)
  private lazy val random = new SecureRandom()

  def encrypt(plainTextBytes: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance(mode)
    random.nextBytes(iv)
    val ivSpec = new IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, sKeySpec, ivSpec)
    iv ++ cipher.doFinal(plainTextBytes)
  }

  def decrypt(encryptedBytes: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance(mode)
    val ivSpec = new IvParameterSpec(encryptedBytes, 0, ivLength)
    cipher.init(Cipher.DECRYPT_MODE, sKeySpec, ivSpec)
    cipher.doFinal(encryptedBytes, ivLength, encryptedBytes.length - ivLength)
  }

  override def toBinary(inputBuff: Array[Byte]): Array[Byte]  = {
    encrypt(inputBuff)
  }
  override def fromBinary(inputBuff: Array[Byte]): Array[Byte] = {
    decrypt(inputBuff)
  }
}

class KryoSerializer(val system: ExtendedActorSystem) extends Serializer {

  import com.romix.akka.serialization.kryo.KryoSerialization._
  val log = Logging(system, getClass.getName)

  val settings = new Settings(system.settings.config)

  val mappings = settings.ClassNameMappings

  locally {
    log.debug("Got mappings: {}", mappings)
  }

  val classnames = settings.ClassNames

  locally {
    log.debug("Got classnames for incremental strategy: {}", classnames)
  }

  val bufferSize = settings.BufferSize

  locally {
    log.debug("Got buffer-size: {}", bufferSize)
  }

  val maxBufferSize = settings.MaxBufferSize

  locally {
    log.debug("Got max-buffer-size: {}", maxBufferSize)
  }

  val idStrategy = settings.IdStrategy

  locally {
    log.debug("Got id strategy: {}", idStrategy)
  }

  val serializerType = settings.SerializerType

  locally {
    log.debug("Got serializer type: {}", serializerType)
  }

  val implicitRegistrationLogging = settings.ImplicitRegistrationLogging
  locally {
    log.debug("Got implicit registration logging: {}", implicitRegistrationLogging)
  }

  val useManifests = settings.UseManifests
  locally {
    log.debug("Got use manifests: {}", useManifests)
  }

  val useUnsafe = settings.UseUnsafe
  locally {
    log.debug("Got use unsafe: {}", useUnsafe)
  }

  val customSerializerInitClassName = settings.KryoCustomSerializerInit
  locally {
    log.debug("Got custom serializer init class: {}", customSerializerInitClassName)
  }

  val customSerializerInitClass =
    if (customSerializerInitClassName == null) null else
      system.dynamicAccess.getClassFor[AnyRef](customSerializerInitClassName) match {
        case Success(clazz) => Some(clazz)
        case Failure(e) =>
          log.error("Class could not be loaded and/or registered: {} ", customSerializerInitClassName)
          throw e
      }

  locally {
    log.debug("Got serializer init class: {}", customSerializerInitClass)
  }

  val customizerInstance = Try(customSerializerInitClass.map(_.newInstance))
  locally {
    log.debug("Got customizer instance: {}", customizerInstance)
  }

  val customizerMethod = Try(customSerializerInitClass.map(_.getMethod("customize", classOf[Kryo])))

  locally {
    log.debug("Got customizer method: {}", customizerMethod)
  }

  val customAESKeyClassName = settings.AESKeyClass
  locally {
    log.debug("Got custom aes key class: {}", customAESKeyClassName)
  }

  val customAESKeyClass =
    if (customAESKeyClassName == null) null else
      system.dynamicAccess.getClassFor[AnyRef](customAESKeyClassName) match {
        case Success(clazz) => Some(clazz)
        case Failure(e) =>
          log.error("Class could not be loaded {} ", customAESKeyClassName)
          throw e
      }
  locally {
    log.debug("Got custom key class: {}", customAESKeyClass)
  }

  val customAESKeyInstance = Try(customAESKeyClass.map(_.newInstance))
  locally {
    log.debug("Got custom aes key instance: {}", customAESKeyInstance)
  }

  val aesKeyMethod = Try(customAESKeyClass.map(_.getMethod("kryoAESKey")))
  locally {
    log.debug("Got custom aes key method: {}", customAESKeyInstance)
  }

  val aesKey = Try(aesKeyMethod.get.get.invoke(customAESKeyInstance.get.get).asInstanceOf[String])
    .getOrElse(settings.AESKey)

  val transform = (typ: String) =>
    typ match {
      case "lz4" => new LZ4KryoCompressor
      case "deflate" => new ZipKryoCompressor
      case "aes" => new KryoCryptographer(aesKey, settings.AESMode, settings.AESIVLength)
      case "off" => new NoKryoTransformer
      case x => throw new Exception(s"Could not recognise the transformer: [$x]")
    }

  val postSerTransformations = {
    settings.PostSerTransformations.split(",").toList.map(transform)
  }

  val kryoTransformer = new KryoTransformer(postSerTransformations)
  locally {
    log.debug("Got transformations: {}", settings.PostSerTransformations)
  }

  val queueBuilder: QueueBuilder =
    if (settings.CustomQueueBuilder == null) null
    else system.dynamicAccess.getClassFor[AnyRef](settings.CustomQueueBuilder) match {
        case Success(clazz) => clazz.newInstance().asInstanceOf[QueueBuilder]
        case Failure(e) =>
          log.error("Class could not be loaded: {} ", settings.CustomQueueBuilder)
          throw e
    }
  locally {
    log.debug("Got queue builder: {}", queueBuilder)
  }

  val serializer = try new KryoBasedSerializer(getKryo(idStrategy, serializerType),
    bufferSize,
    maxBufferSize,
    useManifests,
    useUnsafe)
  catch {
    case e: Exception =>
      log.error("exception caught during akka-kryo-serialization startup: {}", e)
      throw e
  }

  locally {
    log.debug("Got serializer: {}", serializer)
  }

  val resolveSubclasses = settings.ResolveSubclasses

  locally {
    log.debug("Got resolveSubclasses: {}", resolveSubclasses)
  }

  // This is whether "fromBinary" requires a "clazz" or not
  def includeManifest: Boolean = useManifests

  // A unique identifier for this Serializer
  def identifier = 123454323

  // Delegate to a real serializer
  def toBinary(obj: AnyRef): Array[Byte] = {
    val ser = getSerializer
    try
      kryoTransformer.toBinary(ser.toBinary(obj))
    finally
      releaseSerializer(ser)
  }

  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    val ser = getSerializer
    try
      ser.fromBinary(kryoTransformer.fromBinary(bytes), clazz)
    finally
      releaseSerializer(ser)
  }

  val serializerPool = new SerializerPool(queueBuilder, () =>
    new KryoBasedSerializer(getKryo(idStrategy, serializerType),
      bufferSize,
      maxBufferSize,
      useManifests,
      useUnsafe)
  )

  private def getSerializer = serializerPool.fetch()
  private def releaseSerializer(ser: Serializer) = serializerPool.release(ser)

  private def getKryo(strategy: String, serializerType: String): Kryo = {
    val referenceResolver = if (settings.KryoReferenceMap) new MapReferenceResolver() else new ListReferenceResolver()
    val classResolver =
      if (settings.IdStrategy == "incremental") new KryoClassResolver(implicitRegistrationLogging)
      else if (resolveSubclasses) new SubclassResolver()
      else new DefaultClassResolver()
    val kryo = new ScalaKryo(classResolver, referenceResolver, new DefaultStreamFactory())
    kryo.setClassLoader(system.dynamicAccess.classLoader)
    // Support deserialization of classes without no-arg constructors
    val instStrategy = kryo.getInstantiatorStrategy.asInstanceOf[Kryo.DefaultInstantiatorStrategy]
    instStrategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy())
    kryo.setInstantiatorStrategy(instStrategy)
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
    kryo.addDefaultSerializer(classOf[scala.collection.generic.MapFactory[scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaImmutableSetSerializer])

    kryo.addDefaultSerializer(classOf[akka.util.ByteString], classOf[AkkaByteStringSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
    kryo.addDefaultSerializer(classOf[ActorRef], new ActorRefSerializer(system))

    if (settings.KryoTrace)
      MiniLog.TRACE()

    kryo.setRegistrationRequired(strategy == "explicit")

    if (strategy != "default") {

      // register the class mappings and classes
      for ((fqcn: String, idNum: String) <- mappings) {
        val id = idNum.toInt
        // Load class
        system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
          case Success(clazz) => kryo.register(clazz, id)
          case Failure(e) =>
            log.error("Class could not be loaded and/or registered: {} ", fqcn)
            throw e
        }
      }

      for (classname <- classnames.asScala) {
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
      case resolver:SubclassResolver => resolver.enable()
      case _ =>
    }

    kryo
  }


}

/**
 * *
 * Kryo-based serializer backend
 */
class KryoBasedSerializer(
    val kryo: Kryo,
    val bufferSize: Int,
    val maxBufferSize: Int,
    val includeManifest: Boolean,
    val useUnsafe: Boolean) extends Serializer {

  // A unique identifier for this Serializer
  def identifier = 12454323

  // "toBinary" serializes the given object to an Array of Bytes
  def toBinary(obj: AnyRef): Array[Byte] = {
    val buffer = getOutput
    try {
      if (includeManifest)
        kryo.writeObject(buffer, obj)
      else
        kryo.writeClassAndObject(buffer, obj)
      buffer.toBytes
    } finally {
      releaseBuffer(buffer)
    }
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  // into the optionally provided classLoader.
  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    if (includeManifest)
      clazz match {
        case Some(c) => kryo.readObject(getInput(bytes), c).asInstanceOf[AnyRef]
        case _ => throw new RuntimeException("Object of unknown class cannot be deserialized")
      }
    else
      kryo.readClassAndObject(getInput(bytes))
  }

  private[this] val getOutput =
    if (useUnsafe)
      new UnsafeOutput(bufferSize, maxBufferSize)
    else
      new Output(bufferSize, maxBufferSize)

  private def getInput(bytes: Array[Byte]): Input =
    if (useUnsafe)
      new UnsafeInput(bytes)
    else
      new Input(bytes)

  private def releaseBuffer(buffer: Output) = { buffer.clear() }

}

/**
  * Returns a SerializerPool, useful to reduce GC overhead.
  *
  * @param queueBuilder queue builder.
  * @param newInstance  Serializer instance builder.
  */
class SerializerPool(queueBuilder: QueueBuilder, newInstance: () => Serializer) {

  private val pool =
    if (queueBuilder == null)
      new ConcurrentLinkedQueue[Serializer]
    else
      queueBuilder.build

  def fetch(): Serializer = {
    pool.poll() match {
      case o if o != null => o
      case null => newInstance()
    }
  }

  def release(o: Serializer): Unit = {
    pool.offer(o)
  }

  def add(o: Serializer): Unit = {
    pool.add(o)
  }
}

/**
  * Kryo custom queue builder, to replace ConcurrentLinkedQueue for another Queue,
  * Notice that it must be a multiple producer and multiple consumer queue type,
  * you could use for example JCtools MpmcArrayQueue.
  */
trait QueueBuilder {

  def build: util.Queue[Serializer]
}
