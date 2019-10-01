package io.altoo.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable.HashMap

class KryoCryptoTestKey extends DefaultKeyProvider {
  override def aesKey(config: Config) = "TheTestSecretKey"
}

object CryptoCustomKeySerializationTest {
  val config =
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
          key-provider = "io.altoo.akka.serialization.kryo.KryoCryptoTestKey"
          mode = "AES/GCM/PKCS5Padding"
          iv-length = 12
        }
      }
    }
    """
}
class CryptoCustomKeySerializationTest extends FlatSpec with Matchers {
  private val encryptedSystem = ActorSystem("encrypted", ConfigFactory.parseString(CryptoCustomKeySerializationTest.config))
  private val unencryptedSystem = ActorSystem("unencrypted", ConfigFactory.parseString("akka-kryo-serialization.post-serialization-transformations = off").withFallback(ConfigFactory.parseString(CryptoCustomKeySerializationTest.config)))
  private val encryptedSerialization = SerializationExtension(encryptedSystem)
  private val unencryptedSerialization = SerializationExtension(unencryptedSystem)

  it should "encrypt with custom aes key" in {
    val atm = List {
      HashMap[String, Any](
        "foo" -> "foo",
        "bar" -> "foo,bar,baz",
        "baz" -> 124L)
    }.toArray

    val serialized = encryptedSerialization.findSerializerFor(atm).toBinary(atm)
    val decrypted = new KryoCryptographer("TheTestSecretKey", "AES/GCM/PKCS5Padding", 12).fromBinary(serialized)

    val deserialized = unencryptedSerialization.findSerializerFor(atm).fromBinary(decrypted)
    atm shouldBe deserialized
  }

  it should "decrypt with custom aes key" in {
    val atm = List {
      HashMap[String, Any](
        "foo" -> "foo",
        "bar" -> "foo,bar,baz",
        "baz" -> 124L)
    }.toArray

    val serialized = unencryptedSerialization.findSerializerFor(atm).toBinary(atm)
    val encrypted = new KryoCryptographer("TheTestSecretKey", "AES/GCM/PKCS5Padding", 12).toBinary(serialized)

    val deserialized = encryptedSerialization.findSerializerFor(atm).fromBinary(encrypted)
    atm shouldBe deserialized
  }
}