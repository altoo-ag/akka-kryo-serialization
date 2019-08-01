package com.romix.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec

import scala.collection.mutable.AnyRefMap

class KryoCryptoTestKey {
  def kryoAESKey = "TheTestSecretKey"
}

class AkkaKryoCryptoWithCustomKeyTests extends FlatSpec {
  def testConfig(systemName: String, config: String): Unit = {
    val system = ActorSystem("example", ConfigFactory.parseString(config))
    // Get the Serialization Extension
    val serialization = SerializationExtension(system)

    s"$systemName" should "read the aes key from the custom class specified" in {
      val atm = List {
        AnyRefMap[String, Any](
          "foo" -> "foo",
          "bar" -> "foo,bar,baz",
          "baz" -> 124L)
      }.toArray

      val serializer = serialization.findSerializerFor(atm)
      assert(serializer.isInstanceOf[KryoSerializer])
      assert(serializer.asInstanceOf[KryoSerializer].aesKey == (new KryoCryptoTestKey).kryoAESKey)
    }
  }

  testConfig("CustomAESKey", """
      akka {
        extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
        actor {
          kryo {
            type = "nograph"
            idstrategy = "incremental"
            kryo-reference-map = false
            buffer-size = 65536
            post-serialization-transformations = aes
            encryption {
            aes {
              mode = "AES/CBC/PKCS5Padding"
              custom-key-class = "com.romix.akka.serialization.kryo.KryoCryptoTestKey"
                }
              }
            implicit-registration-logging = true
            mappings {
              "scala.collection.immutable.HashMap$HashTrieMap"    = 30
              "[Lscala.collection.immutable.HashMap$HashTrieMap;" = 31
              "scala.collection.mutable.AnyRefMap"                = 34
              "[Lscala.collection.mutable.AnyRefMap;"             = 35
              "scala.collection.mutable.LongMap"                  = 36
              "[Lscala.collection.mutable.LongMap;"               = 37
              "[J" = 50
              "[D" = 51
              "[Z" = 52
              "[Ljava.lang.Object;" = 53
              "[Ljava.lang.String;" = 54
              "scala.math.Ordering$String$" = 100
            }

          }
         serializers {
            kryo = "com.romix.akka.serialization.kryo.KryoSerializer"
          }

          serialization-bindings {
            "scala.collection.mutable.AnyRefMap" = kryo
            "[Lscala.collection.mutable.AnyRefMap;" = kryo
            "scala.collection.mutable.LongMap" = kryo
            "[Lscala.collection.mutable.LongMap;" = kryo
          }
        }
      }
                    """)

}
