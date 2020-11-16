package io.altoo.akka.serialization.kryo.serializer.akka

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.KryoSerializer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ByteStringSerializationTest extends AnyFlatSpec with Matchers {

  private val system = ActorSystem("example", ConfigFactory.parseString(
    """
    akka {
      actor {
        serializers {
          kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
        }
        serialization-bindings {
          "akka.util.ByteString$ByteString1C" = kryo
          "akka.util.ByteString" = kryo
          "scala.collection.immutable.Vector" = kryo
        }
      }
    }
    akka-kryo-serialization {
      trace = true
      id-strategy = "default"
      implicit-registration-logging = true
      post-serialization-transformations = off
    }
    """))

  private val serialization = SerializationExtension(system)


  behavior of "ByteStringSerialization"

  it should "handle Vectors" in {
    test(Vector("foo"))
  }

  it should "handle compact ByteStrings" in {
    test(ByteString("foo").compact)
  }

  private def test(obj: AnyRef): Unit = {
    val serializer = serialization.findSerializerFor(obj)
    (serializer.getClass == classOf[KryoSerializer]) should be(true)
    // Check serialization/deserialization
    val serialized = serialization.serialize(obj)
    serialized.isSuccess should be(true)

    val deserialized = serialization.deserialize(serialized.get, obj.getClass)
    deserialized.isSuccess should be(true)
    (deserialized.get == obj) should be(true)
    deserialized.get.getClass.isAssignableFrom(obj.getClass) should be(true)
    obj.getClass.isAssignableFrom(deserialized.get.getClass) should be(true)
  }
}
