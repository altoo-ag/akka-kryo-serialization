package io.altoo.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.HashMap

object CryptoSerializationTest {
  private val config =
    """
    akka {
      actor {
        serializers {
          kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
        }
        serialization-bindings {
          "scala.collection.immutable.HashMap" = kryo
          "[Lscala.collection.immutable.HashMap;" = kryo
          "scala.collection.mutable.LongMap" = kryo
          "[Lscala.collection.mutable.LongMap;" = kryo
        }
      }
    }
    akka-kryo-serialization {
      post-serialization-transformations = aes
      encryption {
        aes {
          key-provider = "io.altoo.akka.serialization.kryo.DefaultKeyProvider"
          mode = "AES/GCM/PKCS5Padding"
          iv-length = 12
          password = "j68KkRjq21ykRGAQ"
          salt = "pepper"
        }
      }
    }
    """
}
class CryptoSerializationTest extends AnyFlatSpec with Matchers {
  private val sourceSystem = ActorSystem("encrypted", ConfigFactory.parseString(CryptoSerializationTest.config))
  private val targetSystem = ActorSystem("encrypted", ConfigFactory.parseString(CryptoSerializationTest.config))
  private val sourceSerialization = SerializationExtension(sourceSystem)
  private val targetSerialization = SerializationExtension(targetSystem)

  it should "serialize and deserialize with encryption" in {
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
  }
}