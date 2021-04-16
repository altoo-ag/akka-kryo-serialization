package io.altoo.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.{ByteBufferSerializer, SerializationExtension}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import scala.collection.immutable.HashMap

object CryptoSerializationTest {
  private val config =
    """
      |akka {
      |  actor {
      |    serializers {
      |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "scala.collection.immutable.HashMap" = kryo
      |      "[Lscala.collection.immutable.HashMap;" = kryo
      |      "scala.collection.mutable.LongMap" = kryo
      |      "[Lscala.collection.mutable.LongMap;" = kryo
      |    }
      |  }
      |}
      |akka-kryo-serialization {
      |  post-serialization-transformations = aes
      |  encryption {
      |    aes {
      |      key-provider = "io.altoo.akka.serialization.kryo.DefaultKeyProvider"
      |      mode = "AES/GCM/NoPadding"
      |      iv-length = 12
      |      password = "j68KkRjq21ykRGAQ"
      |      salt = "pepper"
      |    }
      |  }
      |}
      |""".stripMargin
}

class CryptoSerializationTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  private val sourceSystem = ActorSystem("source", ConfigFactory.parseString(CryptoSerializationTest.config))
  private val targetSystem = ActorSystem("target", ConfigFactory.parseString(CryptoSerializationTest.config))
  private val sourceSerialization = SerializationExtension(sourceSystem)
  private val targetSerialization = SerializationExtension(targetSystem)

  override protected def afterAll(): Unit = {
    sourceSystem.terminate()
    targetSystem.terminate()
  }


  behavior of "Encrypted serialization"

  it should "serialize and deserialize with encryption accross actor systems" in {
    val atm = List {
      HashMap[String, Any](
        "foo" -> "foo",
        "bar" -> "foo,bar,baz",
        "baz" -> 124L)
    }.toArray

    val serializer = sourceSerialization.findSerializerFor(atm)
    val deserializer = targetSerialization.findSerializerFor(atm)

    val serialized = serializer.toBinary(atm)
    val deserialized = deserializer.fromBinary(serialized)
    atm shouldBe deserialized

    val bufferSerializer = sourceSerialization.findSerializerFor(atm).asInstanceOf[ByteBufferSerializer]
    val bufferDeserializer = targetSerialization.findSerializerFor(atm).asInstanceOf[ByteBufferSerializer]

    val bb = ByteBuffer.allocate(serialized.length * 2)
    bufferSerializer.toBinary(atm, bb)
    bb.flip()
    val bufferDeserialized = bufferDeserializer.fromBinary(bb, atm.getClass.toString)
    atm shouldBe bufferDeserialized
  }
}
