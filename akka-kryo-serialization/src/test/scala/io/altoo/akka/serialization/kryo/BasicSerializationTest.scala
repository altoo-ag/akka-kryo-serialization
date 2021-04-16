package io.altoo.akka.serialization.kryo

import akka.serialization.{ByteBufferSerializer, SerializationExtension}
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.testkit.AbstractAkkaTest

import java.nio.ByteBuffer
import scala.util.Try

object BasicSerializationTest {

  private val config =
    s"""
       |akka {
       |  loggers = ["akka.event.Logging$$DefaultLogger"]
       |  loglevel = "WARNING"
       |
       |  actor {
       |    serializers {
       |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
       |    }
       |
       |    serialization-bindings {
       |      "scala.Product" = kryo
       |    }
       |  }
       |}
       |
       |akka-kryo-serialization {
       |  trace = true
       |  id-strategy = "incremental"
       |  implicit-registration-logging = true
       |  post-serialization-transformations = off
       |}
       |""".stripMargin
}

class BasicSerializationTest extends AbstractAkkaTest(ConfigFactory.parseString(BasicSerializationTest.config)) {
  private val serialization = SerializationExtension(system)

  private val testList = List(1 to 40: _*)

  behavior of "KryoSerializer"

  it should "be selected for lists" in {
    // Find the Serializer for it
    val serializer = serialization.findSerializerFor(testList)
    serializer.getClass.equals(classOf[KryoSerializer]) shouldBe true

    // Check serialization/deserialization
    val serialized = serialization.serialize(testList)
    serialized shouldBe a[util.Success[_]]

    val deserialized = serialization.deserialize(serialized.get, testList.getClass)
    inside(deserialized) {
      case util.Success(v) => v shouldBe testList
    }

    // Check buffer serialization/deserialization
    serializer shouldBe a[ByteBufferSerializer]
    val bufferSerializer = serializer.asInstanceOf[ByteBufferSerializer]

    val bb = ByteBuffer.allocate(testList.length * 8)

    val bufferSerialized = Try(bufferSerializer.toBinary(testList, bb))
    bufferSerialized shouldBe a[util.Success[_]]
    bb.flip()
    val bufferDeserialized = Try(bufferSerializer.fromBinary(bb, testList.getClass.getName))
    inside(bufferDeserialized) {
      case util.Success(v) => v shouldBe testList
    }

  }
}
