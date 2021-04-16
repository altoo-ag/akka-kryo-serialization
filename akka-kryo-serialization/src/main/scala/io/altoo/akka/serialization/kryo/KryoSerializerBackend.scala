package io.altoo.akka.serialization.kryo

import akka.actor.ExtendedActorSystem
import akka.annotation.InternalApi
import akka.event.LoggingAdapter
import akka.serialization.{ByteBufferSerializer, Serializer}
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput, Input, Output}
import com.esotericsoftware.kryo.unsafe.{UnsafeInput, UnsafeOutput}

import java.nio.ByteBuffer
import scala.util.Success

@InternalApi
private[kryo] class KryoSerializerBackend(val kryo: Kryo, val bufferSize: Int, val maxBufferSize: Int, val includeManifest: Boolean, val useUnsafe: Boolean)(system: ExtendedActorSystem, log: LoggingAdapter) extends Serializer with ByteBufferSerializer {
  // A unique identifier for this Serializer
  def identifier = 12454323

  // "toBinary" serializes the given object to an Array of Bytes
  // Implements Serializer
  override def toBinary(obj: AnyRef): Array[Byte] = {
    val buffer = output
    try {
      if (includeManifest)
        kryo.writeObject(buffer, obj)
      else
        kryo.writeClassAndObject(buffer, obj)
      buffer.toBytes
    } catch {
      case e: StackOverflowError if !kryo.getReferences => // when configured with "nograph" serialization can fail with stack overflow
        log.error(e, "Could not serialize class with potentially circular references: {}", obj)
        throw new RuntimeException("Could not serialize class with potential circular references: " + obj)
    } finally {
      buffer.reset()
    }
  }

  // Implements ByteBufferSerializer
  override def toBinary(obj: AnyRef, buf: ByteBuffer): Unit = {
    val buffer = getOutput(buf)
    try {
      if (includeManifest)
        kryo.writeObject(buffer, obj)
      else
        kryo.writeClassAndObject(buffer, obj)
      buffer.toBytes
    } catch {
      case e: StackOverflowError if !kryo.getReferences => // when configured with "nograph" serialization can fail with stack overflow
        log.error(e, "Could not serialize class with potentially circular references: {}", obj)
        throw new RuntimeException("Could not serialize class with potential circular references: " + obj)
    }
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  // into the optionally provided classLoader.
  // Implements Serializer
  override def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    val buffer = getInput(bytes)
    try {
      if (includeManifest)
        clazz match {
          case Some(c) => kryo.readObject(buffer, c).asInstanceOf[AnyRef]
          case _ => throw new RuntimeException("Object of unknown class cannot be deserialized")
        }
      else
        kryo.readClassAndObject(buffer)
    } finally {
      buffer.close()
    }
  }

  // Implements ByteBufferSerializer
  override def fromBinary(buf: ByteBuffer, manifest: String): AnyRef = {
    val buffer = getInput(buf)
    val clazz = system.dynamicAccess.getClassFor[AnyRef](manifest)
    if (includeManifest)
      clazz match {
        case Success(c) => kryo.readObject(buffer, c).asInstanceOf[AnyRef]
        case _ => throw new RuntimeException("Object of unknown class cannot be deserialized")
      }
    else
      kryo.readClassAndObject(buffer)
  }

  // Used by Serializer implementation
  private val output =
    if (useUnsafe)
      new UnsafeOutput(bufferSize, maxBufferSize)
    else
      new Output(bufferSize, maxBufferSize)

  // Used by ByteBufferSerializer implementation
  private def getOutput(buffer: ByteBuffer): Output =
    new ByteBufferOutput(buffer)

  // Used by Serializer implementation
  private def getInput(bytes: Array[Byte]): Input =
    if (useUnsafe)
      new UnsafeInput(bytes)
    else
      new Input(bytes)

  // Used by ByteBufferSerializer implementation
  private def getInput(buffer: ByteBuffer): Input =
    new ByteBufferInput(buffer)

}
