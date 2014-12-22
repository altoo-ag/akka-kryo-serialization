package com.romix.akka.serialization.kryo

import org.scalatest.FlatSpec

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization._
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.AnyRefMap
import scala.collection.mutable.LongMap
import scala.collection.immutable.HashMap

class AkkaKryoCompressionTests211 extends FlatSpec {

  def testConfig(systemName: String, config: String): Unit = {
    val system = ActorSystem("example", ConfigFactory.parseString(config))

    def timeIt[A](name: String, loops: Int)(a: => A) = {
      val now = System.nanoTime
      var i = 0
      while (i < loops) {
        val x = a
        i += 1
      }
      val ms = (System.nanoTime - now) / 1000000
      println(s"$systemName $name:\t$ms\tms\t=\t${loops * 1000 / ms}\tops/s")
    }

    // Get the Serialization Extension
    val serialization = SerializationExtension(system)

    s"$systemName Kryo_2.11" should "serialize and deserialize AnyRefMap[String,Any] successfully" in {
      val r = new scala.util.Random(0L)
      val tm = AnyRefMap[String, Any](
        "foo" -> r.nextDouble,
        "bar" -> "foo,bar,baz",
        "baz" -> 124L,
        "hash" -> HashMap[Int, Int](r.nextInt -> r.nextInt, 5 -> 500, 10 -> r.nextInt))

      assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[AnyRefMap[String, Any]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize Array[AnyRefMap[String,Any]] timings (with compression)" in {
      val iterations = 500
      val listLength = 500

      val r = new scala.util.Random(0L)
      val atm = (List.fill(listLength) {
        AnyRefMap[String, Any](
          "foo" -> r.nextDouble,
          "bar" -> "foo,bar,baz",
          "baz" -> 124L,
          "hash" -> HashMap[Int, Int](r.nextInt -> r.nextInt, 5 -> 500, 10 -> r.nextInt))
      }).toArray

      assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(atm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[AnyRefMap[String, Any]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Mutable AnyRefMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Mutable AnyRefMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Mutable AnyRefMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Mutable AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String, Any]]]))
      timeIt("Mutable AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String, Any]]]))
      timeIt("Mutable AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String, Any]]]))
    }

    it should "serialize and deserialize Array[mutable.LongMap[Any]] timings (with compression)" in {
      val iterations = 500
      val listLength = 500

      val r = new scala.util.Random(0L)
      val atm = (List.fill(listLength) {
        LongMap[Any](
          1L -> r.nextDouble,
          2L -> "foo,bar,baz",
          3L -> 124L,
          4L -> HashMap[Int, Int](r.nextInt -> r.nextInt, 5 -> 500, 10 -> r.nextInt))
      }).toArray

      assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(atm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[LongMap[Any]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Mutable LongMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Mutable LongMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Mutable LongMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Mutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[LongMap[Any]]]))
      timeIt("Mutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[LongMap[Any]]]))
      timeIt("Mutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[LongMap[Any]]]))
    }
  }

  testConfig("Zip", """
      akka {
        extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
        actor {
          kryo {
            type = "nograph"
            idstrategy = "incremental"
            kryo-reference-map = false
            buffer-size = 65536
            compression = deflate
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

  testConfig("LZ4", """
      akka {
        extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
        actor {
          kryo {
            type = "nograph"
            idstrategy = "incremental"
            kryo-reference-map = false
            buffer-size = 65536
            compression = lz4
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

  testConfig("AES", """
      akka {
        extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
        actor {
          kryo {
            type = "nograph"
            idstrategy = "incremental"
            kryo-reference-map = false
            buffer-size = 65536
            encryption = aes
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

  testConfig("Off", """
      akka {
        extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
        actor {
          kryo {
            type = "nograph"
            idstrategy = "incremental"
            kryo-reference-map = false
            buffer-size = 65536
            compression = off
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
