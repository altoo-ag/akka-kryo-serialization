package com.romix.akka.serialization.kryo

import org.scalatest.FlatSpec

import akka.actor.{ ActorRef, ActorSystem }
import akka.serialization._
import com.typesafe.config.ConfigFactory
import scala.collection.immutable.HashMap
import scala.collection.immutable.TreeMap

class AkkaKryoCompressionTests extends FlatSpec {
  val defaultConfig = ConfigFactory.parseString("""
      akka {
        extensions = ["com.romix.akka.serialization.kryo.KryoSerializationExtension$"]
        actor {
          kryo {
            type = "nograph"
            idstrategy = "incremental"
            kryo-reference-map = false
            buffer-size = 65536
            post-serialization-transformations = off
            implicit-registration-logging = true
            mappings {
              "akka.actor.ActorRef" = 20
              "akka.actor.DeadLetterActorRef" = 21
              "scala.collection.immutable.HashMap$HashTrieMap"    = 32
              "[Lscala.collection.immutable.HashMap;"             = 33
              "scala.collection.immutable.TreeMap"                = 34
              "[Lscala.collection.immutable.TreeMap;"             = 35
              "scala.collection.mutable.HashMap"                  = 36
              "[Lscala.collection.mutable.HashMap;"               = 37
              "scala.collection.immutable.HashSet$HashTrieSet"    = 38
              "[Lscala.collection.immutable.HashSet;"             = 39
              "scala.collection.immutable.TreeSet"                = 40
              "[Lscala.collection.immutable.TreeSet;"             = 41
              "scala.collection.mutable.HashSet"                  = 42
              "[Lscala.collection.mutable.HashSet;"               = 43
              "scala.collection.mutable.TreeSet"                  = 44
              "[Lscala.collection.mutable.TreeSet;"               = 45
              "scala.collection.mutable.BitSet"                   = 46
              "[Lscala.collection.mutable.BitSet;"                = 47
              "scala.collection.immutable.BitSet"                 = 48
              "[Lscala.collection.immutable.BitSet;"              = 49
              "scala.collection.immutable.BitSet$BitSet2"         = 50
              "scala.collection.immutable.BitSet$BitSetN"         = 51
              "scala.collection.immutable.BitSet$BitSet1"         = 52

              "[J" = 150
              "[D" = 151
              "[Z" = 152
              "[Ljava.lang.Object;" = 153
              "[Ljava.lang.String;" = 154
              "scala.math.Ordering$String$" = 155
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

            "scala.collection.immutable.HashSet" = kryo
            "[Lscala.collection.immutable.HashSet;" = kryo
            "scala.collection.immutable.TreeSet" = kryo
            "[Lscala.collection.immutable.TreeSet;" = kryo
            "scala.collection.immutable.BitSet" = kryo
            "[Lscala.collection.immutable.BitSet;" = kryo

             "scala.collection.mutable.HashSet" = kryo
            "[Lscala.collection.mutable.HashSet;" = kryo
            "scala.collection.mutable.TreeSet" = kryo
            "[Lscala.collection.mutable.TreeSet;" = kryo
            "scala.collection.mutable.BitSet" = kryo
            "[Lscala.collection.mutable.BitSet;" = kryo
          }
        }
      }
  """)

  def testConfig(systemName: String, config: String): Unit = {
    val system = ActorSystem(systemName, ConfigFactory.parseString(config).withFallback(defaultConfig))

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
      println(s"$systemName $name:\t$ms\tms\t=\t${loops * 1000 / ms}\tops/s")
    }

    // Get the Serialization Extension
    val serialization = SerializationExtension(system)

    // Maps
    s"$systemName KryoSerializer" should "serialize and deserialize immutable TreeMap[String,Any] successfully" in {
      val tm = TreeMap[String, Any](
        "foo" -> 123.3,
        "bar" -> "something as a text",
        "baz" -> null,
        "boom" -> true,
        "hash" -> HashMap[Int, Int](1 -> 200, 2 -> 300, 500 -> 3))

      assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[TreeMap[String, Any]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize immutable HashMap[String,Any] successfully" in {
      val tm = HashMap[String, Any](
        "foo" -> 123.3,
        "bar" -> "something as a text",
        "baz" -> null,
        "boom" -> true,
        "hash" -> HashMap[Int, Int](1 -> 200, 2 -> 300, 500 -> 3))

      assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[HashMap[String, Any]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize mutable HashMap[String,Any] successfully" in {
      val tm = scala.collection.mutable.HashMap[String, Any](
        "foo" -> 123.3,
        "bar" -> "something as a text",
        "baz" -> null,
        "boom" -> true,
        "hash" -> HashMap[Int, Int](1 -> 200, 2 -> 300, 500 -> 3))

      assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.mutable.HashMap[String, Any]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    // Sets
    it should "serialize and deserialize immutable HashSet[String] successfully" in {
      val tm = scala.collection.immutable.HashSet[String]("foo", "bar", "baz", "boom")

      assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.immutable.HashSet[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize immutable TreeSet[String] successfully" in {
      val tm = scala.collection.immutable.TreeSet[String]("foo", "bar", "baz", "boom")

      assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.immutable.TreeSet[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize mutable HashSet[String] successfully" in {
      val tm = scala.collection.mutable.HashSet[String]("foo", "bar", "baz", "boom")

      assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.mutable.HashSet[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize mutable TreeSet[String] successfully" in {
      val tm = scala.collection.mutable.TreeSet[String]("foo", "bar", "baz", "boom")

      assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.mutable.TreeSet[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    // Timing Maps

    it should "serialize and deserialize Array[HashMap[String,Any]] timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val atm = (List.fill(listLength) {
        HashMap[String, Any](
          "foo" -> r.nextDouble,
          "bar" -> "foo,bar,baz",
          "baz" -> 124L,
          "hash" -> HashMap[Int, Int](r.nextInt -> r.nextInt, 5 -> 500, 10 -> r.nextInt))
      }).toArray

      assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(atm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[HashMap[String, Any]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Immutable HashMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Immutable HashMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Immutable HashMap Serialize:   ", iterations) { serialization.serialize(atm) }

      timeIt("Immutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[HashMap[String, Any]]]))
      timeIt("Immutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[HashMap[String, Any]]]))
      timeIt("Immutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[HashMap[String, Any]]]))
    }

    it should "serialize and deserialize Array[TreeMap[String,Any]] timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val atm = (List.fill(listLength) {
        TreeMap[String, Any](
          "foo" -> r.nextDouble,
          "bar" -> "foo,bar,baz",
          "baz" -> 124L,
          "hash" -> HashMap[Int, Int](r.nextInt -> r.nextInt, 5 -> 500, 10 -> r.nextInt))
      }).toArray

      assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(atm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[TreeMap[String, Any]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Immutable TreeMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Immutable TreeMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Immutable TreeMap Serialize:   ", iterations) { serialization.serialize(atm) }

      timeIt("Immutable TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String, Any]]]))
      timeIt("Immutable TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String, Any]]]))
      timeIt("Immutable TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String, Any]]]))
    }

    it should "serialize and deserialize Array[mutable.HashMap[String,Any]] timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val atm = (List.fill(listLength) {
        scala.collection.mutable.HashMap[String, Any](
          "foo" -> r.nextDouble,
          "bar" -> "foo,bar,baz",
          "baz" -> 124L,
          "hash" -> HashMap[Int, Int](r.nextInt -> r.nextInt, 5 -> 500, 10 -> r.nextInt))
      }).toArray

      assert(serialization.findSerializerFor(atm).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(atm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.HashMap[String, Any]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Mutable HashMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Mutable HashMap Serialize:   ", iterations) { serialization.serialize(atm) }
      timeIt("Mutable HashMap Serialize:   ", iterations) { serialization.serialize(atm) }

      timeIt("Mutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String, Any]]]))
      timeIt("Mutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String, Any]]]))
      timeIt("Mutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String, Any]]]))
    }

    // Timing Sets

    it should "serialize and deserialize Array[immutable.HashSet[String]] timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val orig = (List.fill(listLength) {
        scala.collection.immutable.HashSet[String](
          "foo", "bar", "foo,bar,baz", "baz", r.nextString(10))
      }).toArray

      assert(serialization.findSerializerFor(orig).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(orig)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.immutable.HashSet[String]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Immutable HashSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Immutable HashSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Immutable HashSet Serialize:   ", iterations) { serialization.serialize(orig) }

      timeIt("Immutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.HashSet[String]]]))
      timeIt("Immutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.HashSet[String]]]))
      timeIt("Immutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.HashSet[String]]]))
    }

    it should "serialize and deserialize Array[immutable.TreeSet[String]] timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val orig = (List.fill(listLength) {
        scala.collection.immutable.TreeSet[String](
          "foo", "bar", "foo,bar,baz", "baz", r.nextString(10))
      }).toArray

      assert(serialization.findSerializerFor(orig).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(orig)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.immutable.TreeSet[String]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Immutable TreeSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Immutable TreeSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Immutable TreeSet Serialize:   ", iterations) { serialization.serialize(orig) }

      timeIt("Immutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.TreeSet[String]]]))
      timeIt("Immutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.TreeSet[String]]]))
      timeIt("Immutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.TreeSet[String]]]))
    }

    it should "serialize and deserialize Array[immutable.BitSet timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val orig = (List.fill(listLength) {
        scala.collection.immutable.BitSet(
          1, 4, r.nextInt().abs % 32, r.nextInt().abs % 64, r.nextInt().abs % 64, r.nextInt().abs % 128 , r.nextInt().abs % 128,r.nextInt().abs % 256
        )
      }).toArray

      assert(serialization.findSerializerFor(orig).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(orig)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.immutable.BitSet]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Immutable BitSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Immutable BitSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Immutable BitSet Serialize:   ", iterations) { serialization.serialize(orig) }

      timeIt("Immutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.BitSet]]))
      timeIt("Immutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.BitSet]]))
      timeIt("Immutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.BitSet]]))
    }

    it should "serialize and deserialize Array[mutable.HashSet[String]] timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val orig = (List.fill(listLength) {
        scala.collection.mutable.HashSet[String](
          "foo", "bar", "foo,bar,baz", "baz", r.nextString(10))
      }).toArray

      assert(serialization.findSerializerFor(orig).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(orig)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.HashSet[String]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Mutable HashSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Mutable HashSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Mutable HashSet Serialize:   ", iterations) { serialization.serialize(orig) }

      timeIt("Mutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashSet[String]]]))
      timeIt("Mutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashSet[String]]]))
      timeIt("Mutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashSet[String]]]))
    }

    it should "serialize and deserialize Array[mutable.TreeSet[String]] timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val orig = (List.fill(listLength) {
        scala.collection.mutable.TreeSet[String](
          "foo", "bar", "foo,bar,baz", "baz", r.nextString(10))
      }).toArray

      assert(serialization.findSerializerFor(orig).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(orig)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.TreeSet[String]]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Mutable TreeSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Mutable TreeSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Mutable TreeSet Serialize:   ", iterations) { serialization.serialize(orig) }

      timeIt("Mutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.TreeSet[String]]]))
      timeIt("Mutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.TreeSet[String]]]))
      timeIt("Mutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.TreeSet[String]]]))
    }

    it should "serialize and deserialize Array[mutable.BitSet timings (with compression)" in {
      val r = new scala.util.Random(0L)
      val orig = (List.fill(listLength) {
        scala.collection.mutable.BitSet(
          1, 4, r.nextInt().abs % 32, r.nextInt().abs % 64, r.nextInt().abs % 64, r.nextInt().abs % 128 , r.nextInt().abs % 128,r.nextInt().abs % 256
        )
      }).toArray

      assert(serialization.findSerializerFor(orig).getClass === classOf[KryoSerializer])

      val serialized = serialization.serialize(orig)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.BitSet]])
      assert(deserialized.isSuccess)

      val bytes = serialized.get
      println(s"Serialized to ${bytes.length} bytes")

      timeIt("Mutable BitSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Mutable BitSet Serialize:   ", iterations) { serialization.serialize(orig) }
      timeIt("Mutable BitSet Serialize:   ", iterations) { serialization.serialize(orig) }

      timeIt("Mutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.BitSet]]))
      timeIt("Mutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.BitSet]]))
      timeIt("Mutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.BitSet]]))
    }
  }

  testConfig("Zip", "akka.actor.kryo.post-serialization-transformations = deflate")
  testConfig("LZ4", "akka.actor.kryo.post-serialization-transformations = lz4")
  testConfig("AES", "akka.actor.kryo.post-serialization-transformations = aes")
  testConfig("ZipAES",
    """akka.actor.kryo.post-serialization-transformations = "deflate,aes"
      |akka.actor.kryo.encryption.aes.key = j68KkRjq21ykRGAQ
    """.stripMargin)
  testConfig("LZ4AES",
    """akka.actor.kryo.post-serialization-transformations = "lz4,aes"
      |akka.actor.kryo.encryption.aes.key = j68KkRjq21ykRGAQ
    """.stripMargin)
  testConfig("Off", "")
}
