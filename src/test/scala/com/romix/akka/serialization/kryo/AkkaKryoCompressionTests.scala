package com.romix.akka.serialization.kryo

import org.scalatest.FlatSpec

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization._
import com.typesafe.config.ConfigFactory
import scala.collection.immutable.HashMap
import scala.collection.immutable.TreeMap

class AkkaKryoCompressionTests extends FlatSpec {

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
            "akka.actor.ActorRef" = 20
            "akka.actor.DeadLetterActorRef" = 21
            "scala.collection.immutable.HashMap$HashTrieMap"    = 30
            "[Lscala.collection.immutable.HashMap$HashTrieMap;" = 31
            "scala.collection.immutable.HashMap"                = 32
            "[Lscala.collection.immutable.HashMap;"             = 33
            "scala.collection.immutable.TreeMap"                = 34
            "[Lscala.collection.immutable.TreeMap;"             = 35
            "scala.collection.mutable.HashMap"                  = 36
            "[Lscala.collection.mutable.HashMap;"               = 37
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
          "scala.Product" = kryo
          "akka.actor.ActorRef" = kryo
          "scala.collection.immutable.TreeMap" = kryo
          "[Lscala.collection.immutable.TreeMap;" = kryo
          "scala.collection.mutable.HashMap" = kryo
          "[Lscala.collection.mutable.HashMap;" = kryo
          "scala.collection.immutable.HashMap" = kryo
          "[Lscala.collection.immutable.HashMap;" = kryo
        }
      }
    }
"""))

  val iterations = 500
  val listLength = 500

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

  "KryoSerializer" should "serialize and deserialize immutable TreeMap[String,Any] successfully" in {
    val tm = TreeMap[String,Any](
        "foo" -> 123.3,
        "bar" -> "something as a text",
        "baz" -> null,
        "boom"-> true,
        "hash"-> HashMap[Int,Int](1->200,2->300,500->3)
      )

    assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

    val serialized = serialization.serialize(tm)
    assert(serialized.isSuccess)

    val deserialized = serialization.deserialize(serialized.get, classOf[TreeMap[String,Any]])
    assert(deserialized.isSuccess)
    assert(deserialized.get == tm)
  }

  it should "serialize and deserialize mutable HashMap[String,Any] successfully" in {
    val tm = scala.collection.mutable.HashMap[String,Any](
        "foo" -> 123.3,
        "bar" -> "something as a text",
        "baz" -> null,
        "boom"-> true,
        "hash"-> HashMap[Int,Int](1->200,2->300,500->3)
      )

    assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

    val serialized = serialization.serialize(tm)
    assert(serialized.isSuccess)

    val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.mutable.HashMap[String,Any]])
    assert(deserialized.isSuccess)
    assert(deserialized.get == tm)
  }

  it should "serialize and deserialize Array[HashMap[String,Any]] timings (with compression)" in {
    val r  = new scala.util.Random(0L)
    val atm = (List.fill(listLength){ HashMap[String,Any](
            "foo" -> r.nextDouble,
            "bar" -> "foo,bar,baz",
            "baz" -> 124L,
            "hash"-> HashMap[Int,Int](r.nextInt->r.nextInt,5->500, 10->r.nextInt)
        ) }).toArray

    assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

    val serialized = serialization.serialize(atm)
    assert(serialized.isSuccess)

    val deserialized = serialization.deserialize(serialized.get, classOf[Array[HashMap[String,Any]]])
    assert(deserialized.isSuccess)

    val bytes = serialized.get
    println(s"Serialized to ${bytes.length} bytes")

    timeIt("HashMap Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("HashMap Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("HashMap Serialize:   ", iterations){serialization.serialize( atm )}

    timeIt("HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))
    timeIt("HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))
    timeIt("HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))
  }


  it should "serialize and deserialize Array[TreeMap[String,Any]] timings (with compression)" in {
    val r  = new scala.util.Random(0L)
    val atm = (List.fill(listLength){ TreeMap[String,Any](
            "foo" -> r.nextDouble,
            "bar" -> "foo,bar,baz",
            "baz" -> 124L,
            "hash"-> HashMap[Int,Int](r.nextInt->r.nextInt,5->500, 10->r.nextInt)
        ) }).toArray

    assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

    val serialized = serialization.serialize(atm)
    assert(serialized.isSuccess)

    val deserialized = serialization.deserialize(serialized.get, classOf[Array[TreeMap[String,Any]]])
    assert(deserialized.isSuccess)

    val bytes = serialized.get
    println(s"Serialized to ${bytes.length} bytes")

    timeIt("TreeMap Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("TreeMap Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("TreeMap Serialize:   ", iterations){serialization.serialize( atm )}

    timeIt("TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))
    timeIt("TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))
    timeIt("TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))
  }

  it should "serialize and deserialize Array[mutable.HashMap[String,Any]] timings (with compression)" in {
    val r  = new scala.util.Random(0L)
    val atm = (List.fill(listLength){ scala.collection.mutable.HashMap[String,Any](
            "foo" -> r.nextDouble,
            "bar" -> "foo,bar,baz",
            "baz" -> 124L,
            "hash"-> HashMap[Int,Int](r.nextInt->r.nextInt,5->500, 10->r.nextInt)
        ) }).toArray

    assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

    val serialized = serialization.serialize(atm)
    assert(serialized.isSuccess)

    val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.HashMap[String,Any]]])
    assert(deserialized.isSuccess)

    val bytes = serialized.get
    println(s"Serialized to ${bytes.length} bytes")

    timeIt("Mutable Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("Mutable Serialize:   ", iterations){serialization.serialize( atm )}
    timeIt("Mutable Serialize:   ", iterations){serialization.serialize( atm )}

    timeIt("Mutable Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String,Any]]]))
    timeIt("Mutable Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String,Any]]]))
    timeIt("Mutable Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String,Any]]]))
  }
}
