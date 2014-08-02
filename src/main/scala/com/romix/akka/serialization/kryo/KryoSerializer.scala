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

import akka.serialization._
import akka.actor.ExtendedActorSystem
import akka.actor.ActorRef
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.FieldSerializer
import org.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.util._
import com.romix.scala.serialization.kryo._
import scala.util.{ Try, Success, Failure }
import KryoSerialization._
import com.esotericsoftware.minlog.{ Log => MiniLog }
import com.romix.scala.serialization.kryo.ScalaKryo
import net.jpountz.lz4.LZ4Factory
import java.util.zip.{ Deflater, Inflater }
import scala.collection.mutable.ArrayBuilder

trait KryoCompressor {
  def compress(inputBuff: Array[Byte]): Array[Byte]
  def decompress(inputBuff: Array[Byte]): Array[Byte]
}

class NoKryoComressor extends KryoCompressor {
  def compress(inputBuff: Array[Byte]) = inputBuff
  def decompress(inputBuff: Array[Byte]) = inputBuff
}

class LZ4KryoComressor extends KryoCompressor {

  lazy val lz4factory = LZ4Factory.fastestInstance

  def compress(inputBuff: Array[Byte]): Array[Byte] = {
    val inputSize = inputBuff.length
    val lz4 = lz4factory.fastCompressor
    val maxOutputSize = lz4.maxCompressedLength(inputSize);
    val outputBuff = new Array[Byte](maxOutputSize + 4)
    val outputSize = lz4.compress(inputBuff, 0, inputSize, outputBuff, 4, maxOutputSize)

    // encode 32 bit lenght in the first bytes
    outputBuff(0) = (inputSize & 0xff).toByte
    outputBuff(1) = (inputSize >> 8 & 0xff).toByte
    outputBuff(2) = (inputSize >> 16 & 0xff).toByte
    outputBuff(3) = (inputSize >> 24 & 0xff).toByte
    outputBuff.take(outputSize + 4)
  }

  def decompress(inputBuff: Array[Byte]): Array[Byte] = {
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

class ZipKryoComressor extends KryoCompressor {

  lazy val deflater = new Deflater(Deflater.BEST_SPEED)
  lazy val inflater = new Inflater()

  def compress(inputBuff: Array[Byte]): Array[Byte] = {
    val inputSize = inputBuff.length
    val outputBuff = new ArrayBuilder.ofByte
    outputBuff += (inputSize & 0xff).toByte
    outputBuff += (inputSize >> 8 & 0xff).toByte
    outputBuff += (inputSize >> 16 & 0xff).toByte
    outputBuff += (inputSize >> 24 & 0xff).toByte

    deflater.setInput(inputBuff)
    deflater.finish
    val buff = new Array[Byte](4096)

    while (!deflater.finished) {
      val n = deflater.deflate(buff)
      outputBuff ++= buff.take(n)
    }
    deflater.reset
    outputBuff.result
  }

  def decompress(inputBuff: Array[Byte]): Array[Byte] = {
    val size: Int = (inputBuff(0).asInstanceOf[Int] & 0xff) |
      (inputBuff(1).asInstanceOf[Int] & 0xff) << 8 |
      (inputBuff(2).asInstanceOf[Int] & 0xff) << 16 |
      (inputBuff(3).asInstanceOf[Int] & 0xff) << 24
    val outputBuff = new Array[Byte](size)
    inflater.setInput(inputBuff, 4, inputBuff.length - 4)
    inflater.inflate(outputBuff)
    inflater.reset
    outputBuff
  }
}

class KryoSerializer(val system: ExtendedActorSystem) extends Serializer {

  import KryoSerialization._
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
  
  val serializerPoolSize = settings.SerializerPoolSize

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

  val customSerializerInitClassName = settings.KryoCustomSerializerInit
  locally {
    log.debug("Got custom serializer init class: {}", customSerializerInitClassName)
  }

  val customSerializerInitClass =
    if (customSerializerInitClassName == null) null else
      system.dynamicAccess.getClassFor[AnyRef](customSerializerInitClassName) match {
        case Success(clazz) => Some(clazz)
        case Failure(e) => {
          log.error("Class could not be loaded and/or registered: {} ", customSerializerInitClassName)
          throw e
        }
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
  val compressor: KryoCompressor = settings.Compression match {
    case "lz4" => new LZ4KryoComressor
    case "deflate" => new ZipKryoComressor
    case _ => new NoKryoComressor
  }
  locally {
    log.debug("Got compression: {}", settings.Compression)
  }

  val serializer = try new KryoBasedSerializer(getKryo(idStrategy, serializerType),
    bufferSize,
    maxBufferSize,
    serializerPoolSize,
    useManifests)
  catch {
    case e: Exception => {
      log.error("exception caught during akka-kryo-serialization startup: {}", e)
      throw e
    }
  }

  locally {
    log.debug("Got serializer: {}", serializer)
  }

  // This is whether "fromBinary" requires a "clazz" or not
  def includeManifest: Boolean = useManifests

  // A unique identifier for this Serializer
  def identifier = 123454323

  // Delegate to a real serializer
  def toBinary(obj: AnyRef): Array[Byte] = {
    val ser = getSerializer
    val bin = ser.toBinary(obj)
    releaseSerializer(ser)
    compressor.compress(bin)
  }

  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    val ser = getSerializer
    val obj = ser.fromBinary(compressor.decompress(bytes), clazz)
    releaseSerializer(ser)
    obj
  }

  val serializerPool = new ObjectPool[Serializer](serializerPoolSize, () => {
    new KryoBasedSerializer(getKryo(idStrategy, serializerType),
      bufferSize,
      maxBufferSize,
      serializerPoolSize,
      useManifests)
  })

  private def getSerializer = serializerPool.fetch
  private def releaseSerializer(ser: Serializer) = serializerPool.release(ser)

  private def getKryo(strategy: String, serializerType: String): Kryo = {
    val referenceResolver = if (settings.KryoReferenceMap) new MapReferenceResolver() else new ListReferenceResolver()
    val kryo = new ScalaKryo(new KryoClassResolver(implicitRegistrationLogging), referenceResolver, new DefaultStreamFactory())
    // Support deserialization of classes without no-arg constructors
    val instStrategy = kryo.getInstantiatorStrategy().asInstanceOf[Kryo.DefaultInstantiatorStrategy]
    instStrategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy())
    kryo.setInstantiatorStrategy(instStrategy)
    // Support serialization of some standard or often used Scala classes
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationSerializer])
    system.dynamicAccess.getClassFor[AnyRef]("scala.Enumeration$Val") match {
      case Success(clazz) => kryo.register(clazz)
      case Failure(e) => {
        log.error("Class could not be loaded and/or registered: {} ", "scala.Enumeration$Val")
        throw e
      }
    }
    kryo.register(classOf[scala.Enumeration#Value])
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

    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
    kryo.addDefaultSerializer(classOf[ActorRef], new ActorRefSerializer(system))

    if (settings.KryoTrace)
      MiniLog.TRACE()

    strategy match {
      case "default" => {}

      case "incremental" => {
        kryo.setRegistrationRequired(false)

        for ((fqcn: String, idNum: String) <- mappings) {
          val id = idNum.toInt
          // Load class
          system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
            case Success(clazz) => kryo.register(clazz, id)
            case Failure(e) => {
              log.error("Class could not be loaded and/or registered: {} ", fqcn)
              throw e
            }
          }
        }

        for (classname <- classnames) {
          // Load class
          system.dynamicAccess.getClassFor[AnyRef](classname) match {
            case Success(clazz) => kryo.register(clazz)
            case Failure(e) => {
              log.warning("Class could not be loaded and/or registered: {} ", classname)
              /* throw e */
            }
          }
        }
      }

      case "explicit" => {
        kryo.setRegistrationRequired(false)

        for ((fqcn: String, idNum: String) <- mappings) {
          val id = idNum.toInt
          // Load class
          system.dynamicAccess.getClassFor[AnyRef](fqcn) match {
            case Success(clazz) => kryo.register(clazz, id)
            case Failure(e) => {
              log.error("Class could not be loaded and/or registered: {} ", fqcn)
              throw e
            }
          }
        }

        for (classname <- classnames) {
          // Load class
          system.dynamicAccess.getClassFor[AnyRef](classname) match {
            case Success(clazz) => kryo.register(clazz)
            case Failure(e) => {
              log.warning("Class could not be loaded and/or registered: {} ", classname)
              /* throw e */
            }
          }
        }
        kryo.setRegistrationRequired(true)
      }
    }

    serializerType match {
      case "graph" => kryo.setReferences(true)
      case _ => kryo.setReferences(false)
    }

    Try(customizerMethod.get.get.invoke(customizerInstance.get.get, kryo))

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
    val bufferPoolSize: Int,
    val useManifests: Boolean) extends Serializer {

  // This is whether "fromBinary" requires a "clazz" or not
  def includeManifest: Boolean = useManifests

  // A unique identifier for this Serializer
  def identifier = 12454323

  // "toBinary" serializes the given object to an Array of Bytes
  def toBinary(obj: AnyRef): Array[Byte] = {
    val buffer = getBuffer
    try {
      if (!useManifests)
        kryo.writeClassAndObject(buffer, obj)
      else
        kryo.writeObject(buffer, obj)
      buffer.toBytes()
    } finally
      releaseBuffer(buffer)
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  // into the optionally provided classLoader.
  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    if (!useManifests)
      kryo.readClassAndObject(new Input(bytes)).asInstanceOf[AnyRef]
    else {
      clazz match {
        case Some(c) => kryo.readObject(new Input(bytes), c).asInstanceOf[AnyRef]
        case _ => throw new RuntimeException("Object of unknown class cannot be deserialized")
      }
    }
  }

  val buf = new Output(bufferSize, maxBufferSize)
  private def getBuffer = buf
  private def releaseBuffer(buffer: Output) = { buffer.clear() }

}

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

// Support pooling of objects. Useful if you want to reduce
// the GC overhead and memory pressure.
class ObjectPool[T](number: Int, newInstance: () => T) {

  private val size = new AtomicInteger(0)
  private val pool = new ArrayBlockingQueue[T](number)

  def fetch(): T = {
    pool.poll() match {
      case o if o != null => o
      case null => createOrBlock
    }
  }

  def release(o: T): Unit = {
    pool.offer(o)
  }

  def add(o: T): Unit = {
    pool.add(o)
  }

  private def createOrBlock: T = {
    size.get match {
      case e: Int if e == number => block
      case _ => create
    }
  }

  private def create: T = {
    size.incrementAndGet match {
      case e: Int if e > number =>
        size.decrementAndGet; fetch()
      case e: Int => newInstance()
    }
  }

  private def block: T = {
    val timeout = 5000
    pool.poll(timeout, TimeUnit.MILLISECONDS) match {
      case o if o != null => o
      case _ => throw new Exception("Couldn't acquire object in %d milliseconds.".format(timeout))
    }
  }
}
