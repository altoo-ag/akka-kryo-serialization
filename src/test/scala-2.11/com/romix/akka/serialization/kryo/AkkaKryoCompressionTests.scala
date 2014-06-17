package com.romix.akka.serialization.kryo

import org.scalatest.FlatSpec

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization._
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.AnyRefMap
import scala.collection.immutable.LongMap


class AkkaKryoCompressionTests211 extends FlatSpec {

  val system = ActorSystem("example", ConfigFactory.parseString("""
    akka {
      extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
      actor {
        kryo {
          type = "nograph"
          idstrategy = "incremental"
          kryo-reference-map = false
          buffer-size = 65536
          enable-compression = true
          implicit-registration-logging = true
          mappings {
            "scala.collection.immutable.HashMap$HashTrieMap"    = 30
            "[Lscala.collection.immutable.HashMap$HashTrieMap;" = 31
            "scala.collection.mutable.AnyRefMap"                = 34
            "[Lscala.collection.mutable.AnyRefMap;"             = 35
            "scala.collection.immutable.LongMap$Bin"            = 36
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
        }
      }
    }
"""))

  def timeIt[A](name: String, loops: Int)(a: => A) = {
    val now = System.nanoTime
    var i = 0
    while (i < loops) {
      val x = a
      i += 1
    }
    val ms = (System.nanoTime - now) / 1000000
    println(s"$name:\t$ms\tms\t=\t${loops*1000/ms}\tops/s")
  }

  // Get the Serialization Extension
  val serialization = SerializationExtension(system)

  "Kryo_2.11" should "serialize and deserialize AnyRefMap[String,Any] successfully" in {
    val tm = AnyRefMap[String,Any](
        "foo" -> 123.3,
        "bar" -> "something as a text",
        "baz" -> 23L,
        "boom"-> true,
        "hash"-> LongMap[Int](1L->200,2L->300,500L->3)
      )

    assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
    val serialized = serialization.serialize(tm)
    assert(serialized.isSuccess)

    val deserialized = serialization.deserialize(serialized.get, classOf[AnyRefMap[String,Any]])
    assert(deserialized.isSuccess)
    assert(deserialized.get == tm)
  }


  it should "serialize and deserialize Array[AnyRefMap[String,Any]] timings (with compression)" in {
    val iterations = 500
    val listLength = 500

    val r  = new scala.util.Random(0L)
    val atm = (List.fill(listLength){ AnyRefMap[String,Any](
            "foo" -> r.nextDouble,
            "bar" -> "foo,bar,baz",
            "baz" -> 124L,
            "hash"-> LongMap[Int](r.nextLong->r.nextInt,5L->500, 10L->r.nextInt)
        ) }).toArray

    assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

    val serialized = serialization.serialize(atm)
    assert(serialized.isSuccess)

    val deserialized = serialization.deserialize(serialized.get, classOf[Array[AnyRefMap[String,Any]]])
    assert(deserialized.isSuccess)

    val bytes = serialized.get
    println(s"Serialized to ${bytes.length} bytes")

    timeIt("AnyRefMap Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("AnyRefMap Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("AnyRefMap Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String,Any]]]))
    timeIt("AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String,Any]]]))
    timeIt("AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String,Any]]]))
  }
}
