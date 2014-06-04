package com.romix.akka.serialization.kryo

import org.specs._

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization._
import com.typesafe.config.ConfigFactory
import scala.collection.immutable.HashMap
import scala.collection.immutable.TreeMap

class AkkaKryoCompressionTests extends Specification {

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
            "scala.collection.immutable.HashMap$HashTrieMap" = 30
            "[Lscala.collection.immutable.HashMap$HashTrieMap;" = 31
            "scala.collection.immutable.TreeMap"                = 32
            "[Lscala.collection.immutable.TreeMap;"             = 33
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
        }
      }
    }
"""))
  // Get the Serialization Extension
  val serialization = SerializationExtension(system)

  "KryoSerializer" should {
    "serialize and deserialize TreeMap[String,Any] successfully" in {
      val tm = TreeMap[String,Any](
          "foo" -> 123.3,
          "bar" -> "something as a text",
          "baz" -> 23L,
          "boom"-> true,
          "hash"-> HashMap[Int,Int](1->200,2->300,500->3)
        )
      serialization.findSerializerFor(tm).getClass.equals(classOf[KryoSerializer]) must beTrue

      val serialized = serialization.serialize(tm)
      serialized.isSuccess must beTrue

      val deserialized = serialization.deserialize(serialized.get, classOf[TreeMap[String,Any]])
      deserialized.isSuccess must beTrue

      deserialized.get.equals(tm) must beTrue
    }

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

    "serialize and deserialize Array[TreeMap[String,Any]] timings (with compression)" in {
      val iterations = 500
      val listLength = 500

      val r  = new scala.util.Random(0L)
      val atm = (List.fill(listLength){ TreeMap[String,Any](
              "foo" -> r.nextDouble,
              "bar" -> "foo,bar,baz",
              "baz" -> 124L,
              "goo" -> r.nextString(50),
              "hash"-> HashMap[Int,Int](r.nextInt->r.nextInt,5->500, r.nextInt->r.nextInt)
          ) }).toArray

      serialization.findSerializerFor(atm).getClass.equals(classOf[KryoSerializer]) must beTrue

      val serialized = serialization.serialize(atm)
      serialized.isSuccess must beTrue

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[TreeMap[String,Any]]])
      deserialized.isSuccess must beTrue

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Serialize:   ", iterations){serialization.serialize( atm )}
      timeIt("Serialize:   ", iterations){serialization.serialize( atm )}
      timeIt("Serialize:   ", iterations){serialization.serialize( atm )}

      timeIt("Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))
      timeIt("Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))
      timeIt("Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String,Any]]]))

    }
  }
}
