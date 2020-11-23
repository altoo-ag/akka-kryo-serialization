package io.altoo.akka.serialization.kryo.serializer.akka

import akka.serialization.SerializationExtension
import akka.util.{ByteString, CompactByteString}
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.KryoSerializer
import io.altoo.akka.serialization.kryo.testkit.AbstractAkkaTest
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

object ByteStringSerializerTest {
  private val config =
    """
      |akka {
      |  actor {
      |    serializers {
      |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "akka.util.ByteString$ByteString1C" = kryo
      |      "akka.util.ByteString" = kryo
      |    }
      |  }
      |}
      |akka-kryo-serialization {
      |  trace = true
      |  id-strategy = "default"
      |  implicit-registration-logging = true
      |  post-serialization-transformations = off
      |}
      |""".stripMargin
}

class ByteStringSerializerTest extends AbstractAkkaTest(ConfigFactory.parseString(ByteStringSerializerTest.config)) with AnyFlatSpecLike with Matchers {
  private val serialization = SerializationExtension(system)


  behavior of "ByteStringSerializer"

  it should "handle ByteStrings" in {
    val value = ByteString("foo")

    // serialize
    val serializer = serialization.findSerializerFor(value)
    serializer shouldBe a[KryoSerializer]

    val serialized = serialization.serialize(value)
    serialized.isSuccess shouldBe true

    // deserialize
    val deserialized = serialization.deserialize(serialized.get, classOf[ByteString])
    deserialized.isSuccess shouldBe true
    deserialized.get shouldBe value
  }

  it should "handle compact ByteStrings" in {
    val value = ByteString("foo").compact

    // serialize
    val serializer = serialization.findSerializerFor(value)
    serializer shouldBe a[KryoSerializer]

    val serialized = serialization.serialize(value)
    serialized.isSuccess shouldBe true

    // deserialize
    val deserialized = serialization.deserialize(serialized.get, classOf[CompactByteString])
    deserialized.isSuccess shouldBe true
    deserialized.get shouldBe value
  }
}
