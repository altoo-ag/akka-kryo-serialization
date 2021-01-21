package io.altoo.akka.serialization.kryo.performance

import akka.actor.ActorSystem
import akka.serialization._
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.KryoSerializer
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.immutable.{HashMap, TreeMap}
import scala.collection.mutable.{AnyRefMap, LongMap}

object CompressionPerformanceTests {
  def main(args: Array[String]): Unit = {
    (new PerformanceTests).execute()
  }

  private class PerformanceTests extends AnyFlatSpec with BeforeAndAfterAllConfigMap {
    private val defaultConfig = ConfigFactory.parseString(
      """
      akka {
        actor {
          allow-java-serialization = true

          serializers {
            java = "akka.serialization.JavaSerializer"
            kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
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

            "scala.collection.mutable.AnyRefMap" = kryo
            "[Lscala.collection.mutable.AnyRefMap;" = kryo

            "scala.collection.immutable.LongMap" = kryo
            "[Lscala.collection.immutable.LongMap;" = kryo

            "scala.collection.mutable.LongMap" = kryo
            "[Lscala.collection.mutable.LongMap;" = kryo

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

            "scala.collection.immutable.Vector" = kryo
            "[Lscala.collection.immutable.Vector;" = kryo

            "[Ljava.lang.Object;" = kryo
            "[[I" = kryo
          }
        }
      }
      akka-kryo-serialization {
        type = "nograph"
        id-strategy = "incremental"
        kryo-reference-map = false
        buffer-size = 65536
        post-serialization-transformations = off
        implicit-registration-logging = true
        encryption {
          aes {
            key-provider = "io.altoo.akka.serialization.kryo.DefaultKeyProvider"
            mode = "AES/GCM/NoPadding"
            iv-length = 12
            password = "j68KkRjq21ykRGAQ"
            salt = "pepper"
          }
        }
      }
  """)

    var iterations: Int = 1000
    var listLength: Int = 500

    override def beforeAll(configMap: ConfigMap): Unit = {
      configMap.getOptional[String]("iterations")
          .foreach { i => iterations = i.toInt }
      configMap.getOptional[String]("listLength")
          .foreach { ll => listLength = ll.toInt }
    }

    def testConfig(systemName: String, config: String): Unit = {
      val system = ActorSystem(systemName, ConfigFactory.parseString(config).withFallback(defaultConfig))

      def timeIt[A](name: String, loops: Int)(a: => A) = {
        val now = System.nanoTime
        var i = 0
        while (i < loops) {
          a
          i += 1
        }
        val ms = (System.nanoTime - now) / 1000000.0
        println(f"$systemName%s $name%s\t$ms%.1f\tms\t=\t${loops * listLength / ms}%.0f\tops/ms")
      }

      // Get the Serialization Extension
      val serialization = SerializationExtension(system)


      // Timing Maps
      s"$systemName KryoSerializer" should "serialize and deserialize Array[HashMap[String,Any]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val atm = Array.fill(listLength) {
          HashMap[String, Any](
            "foo" -> r.nextDouble(),
            "bar" -> "foo,bar,baz",
            "baz" -> 124L
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(atm).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(atm).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(atm)
        assert(serialized.isSuccess, serialized)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[HashMap[String, Any]]])
        assert(deserialized.isSuccess, deserialized)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Immutable HashMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Immutable HashMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Immutable HashMap Serialize:   ", iterations) {serialization.serialize(atm)}

        timeIt("Immutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[HashMap[String, Any]]]))
        timeIt("Immutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[HashMap[String, Any]]]))
        timeIt("Immutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[HashMap[String, Any]]]))
      }

      it should "serialize and deserialize Array[TreeMap[String,Any]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val atm = Array.fill(listLength) {
          TreeMap[String, Any](
            "foo" -> r.nextDouble(),
            "bar" -> "foo,bar,baz",
            "baz" -> 124L
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(atm).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(atm).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(atm)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[TreeMap[String, Any]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Immutable TreeMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Immutable TreeMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Immutable TreeMap Serialize:   ", iterations) {serialization.serialize(atm)}

        timeIt("Immutable TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String, Any]]]))
        timeIt("Immutable TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String, Any]]]))
        timeIt("Immutable TreeMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[TreeMap[String, Any]]]))
      }

      it should "serialize and deserialize Array[immutable.LongMap[Any]] timings (with compression)" in {

        val r = new scala.util.Random(0L)
        val atm = Array.fill(listLength) {
          LongMap[Any](
            1L -> r.nextDouble(),
            2L -> "foo,bar,baz",
            3L -> 124L
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(atm).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(atm).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(atm)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[collection.immutable.LongMap[Any]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Immutable LongMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Immutable LongMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Immutable LongMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Immutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[collection.immutable.LongMap[Any]]]))
        timeIt("Immutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[collection.immutable.LongMap[Any]]]))
        timeIt("Immutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[collection.immutable.LongMap[Any]]]))
      }

      it should "serialize and deserialize Array[AnyRefMap[String,Any]] timings (with compression)" in {
        if (systemName != "Java") { // AnyRefMap can't be serialized by the Java serializer

          val r = new scala.util.Random(0L)
          val atm = Array.fill(listLength) {
            AnyRefMap[String, Any](
              "foo" -> r.nextDouble(),
              "bar" -> "foo,bar,baz",
              "baz" -> 124L
            )
          }

          assert(serialization.findSerializerFor(atm).getClass == classOf[KryoSerializer])

          val serialized = serialization.serialize(atm)
          assert(serialized.isSuccess)

          val deserialized = serialization.deserialize(serialized.get, classOf[Array[AnyRefMap[String, Any]]])
          assert(deserialized.isSuccess)

          val bytes = serialized.get
          println(s"Serialized to ${bytes.length} bytes")

          timeIt("Mutable AnyRefMap Serialize:   ", iterations) {serialization.serialize(atm)}
          timeIt("Mutable AnyRefMap Serialize:   ", iterations) {serialization.serialize(atm)}
          timeIt("Mutable AnyRefMap Serialize:   ", iterations) {serialization.serialize(atm)}
          timeIt("Mutable AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String, Any]]]))
          timeIt("Mutable AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String, Any]]]))
          timeIt("Mutable AnyRefMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[AnyRefMap[String, Any]]]))
        }
      }

      it should "serialize and deserialize Array[mutable.HashMap[String,Any]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val atm = Array.fill(listLength) {
          scala.collection.mutable.HashMap[String, Any](
            "foo" -> r.nextDouble(),
            "bar" -> "foo,bar,baz",
            "baz" -> 124L
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(atm).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(atm).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(atm)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.HashMap[String, Any]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Mutable HashMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Mutable HashMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Mutable HashMap Serialize:   ", iterations) {serialization.serialize(atm)}

        timeIt("Mutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String, Any]]]))
        timeIt("Mutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String, Any]]]))
        timeIt("Mutable HashMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashMap[String, Any]]]))
      }

      it should "serialize and deserialize Array[mutable.LongMap[Any]] timings (with compression)" in {

        val r = new scala.util.Random(0L)
        val atm = Array.fill(listLength) {
          LongMap[Any](
            1L -> r.nextDouble(),
            2L -> "foo,bar,baz",
            3L -> 124L
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(atm).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(atm).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(atm)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[LongMap[Any]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Mutable LongMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Mutable LongMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Mutable LongMap Serialize:   ", iterations) {serialization.serialize(atm)}
        timeIt("Mutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[LongMap[Any]]]))
        timeIt("Mutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[LongMap[Any]]]))
        timeIt("Mutable LongMap Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[LongMap[Any]]]))
      }

      // Timing Sets

      it should "serialize and deserialize Array[immutable.HashSet[String]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val orig = Array.fill(listLength) {
          scala.collection.immutable.HashSet[String](
            "foo", "bar", "foo,bar,baz", "baz", r.nextString(10))
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(orig).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(orig).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(orig)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.immutable.HashSet[String]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Immutable HashSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Immutable HashSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Immutable HashSet Serialize:   ", iterations) {serialization.serialize(orig)}

        timeIt("Immutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.HashSet[String]]]))
        timeIt("Immutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.HashSet[String]]]))
        timeIt("Immutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.HashSet[String]]]))
      }

      it should "serialize and deserialize Array[immutable.TreeSet[String]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val orig = Array.fill(listLength) {
          scala.collection.immutable.TreeSet[String](
            "foo", "bar", "foo,bar,baz", "baz", r.nextString(10))
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(orig).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(orig).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(orig)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.immutable.TreeSet[String]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Immutable TreeSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Immutable TreeSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Immutable TreeSet Serialize:   ", iterations) {serialization.serialize(orig)}

        timeIt("Immutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.TreeSet[String]]]))
        timeIt("Immutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.TreeSet[String]]]))
        timeIt("Immutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.TreeSet[String]]]))
      }

      it should "serialize and deserialize Array[immutable.BitSet timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val orig = Array.fill(listLength) {
          scala.collection.immutable.BitSet(
            1, 4, r.nextInt().abs % 32, r.nextInt().abs % 64, r.nextInt().abs % 64, r.nextInt().abs % 128, r.nextInt().abs % 128, r.nextInt().abs % 256
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(orig).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(orig).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(orig)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.immutable.BitSet]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Immutable BitSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Immutable BitSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Immutable BitSet Serialize:   ", iterations) {serialization.serialize(orig)}

        timeIt("Immutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.BitSet]]))
        timeIt("Immutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.BitSet]]))
        timeIt("Immutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.immutable.BitSet]]))
      }

      it should "serialize and deserialize Array[mutable.HashSet[String]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val orig = Array.fill(listLength) {
          scala.collection.mutable.HashSet[String](
            "foo", "bar", "foo,bar,baz", "baz", r.nextString(10))
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(orig).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(orig).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(orig)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.HashSet[String]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Mutable HashSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Mutable HashSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Mutable HashSet Serialize:   ", iterations) {serialization.serialize(orig)}

        timeIt("Mutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashSet[String]]]))
        timeIt("Mutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashSet[String]]]))
        timeIt("Mutable HashSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.HashSet[String]]]))
      }

      it should "serialize and deserialize Array[mutable.TreeSet[String]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val orig = Array.fill(listLength) {
          scala.collection.mutable.TreeSet[String](
            "foo", "bar", "foo,bar,baz", "baz", r.nextString(10))
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(orig).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(orig).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(orig)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.TreeSet[String]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Mutable TreeSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Mutable TreeSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Mutable TreeSet Serialize:   ", iterations) {serialization.serialize(orig)}

        timeIt("Mutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.TreeSet[String]]]))
        timeIt("Mutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.TreeSet[String]]]))
        timeIt("Mutable TreeSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.TreeSet[String]]]))
      }

      it should "serialize and deserialize Array[mutable.BitSet timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val orig = Array.fill(listLength) {
          scala.collection.mutable.BitSet(
            1, 4, r.nextInt().abs % 32, r.nextInt().abs % 64, r.nextInt().abs % 64, r.nextInt().abs % 128, r.nextInt().abs % 128, r.nextInt().abs % 256
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(orig).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(orig).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(orig)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.BitSet]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Mutable BitSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Mutable BitSet Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Mutable BitSet Serialize:   ", iterations) {serialization.serialize(orig)}

        timeIt("Mutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.BitSet]]))
        timeIt("Mutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.BitSet]]))
        timeIt("Mutable BitSet Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[scala.collection.mutable.BitSet]]))
      }

      it should "serialize and deserialize Array[Vector[Int]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val orig = Array.fill(listLength) {
          Vector(
            1, 4, r.nextInt().abs % 32, r.nextInt().abs % 64, r.nextInt().abs % 64, r.nextInt().abs % 128, r.nextInt().abs % 128, r.nextInt().abs % 256
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(orig).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(orig).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(orig)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[scala.collection.mutable.BitSet]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Vector Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Vector Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Vector Serialize:   ", iterations) {serialization.serialize(orig)}

        timeIt("Vector Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[Vector[Int]]]))
        timeIt("Vector Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[Vector[Int]]]))
        timeIt("Vector Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[Vector[Int]]]))
      }

      it should "serialize and deserialize Array[Array[Int]] timings (with compression)" in {
        val r = new scala.util.Random(0L)
        val orig = Array.fill(listLength) {
          Array(
            1, 4, r.nextInt().abs % 32, r.nextInt().abs % 64, r.nextInt().abs % 64, r.nextInt().abs % 128, r.nextInt().abs % 128, r.nextInt().abs % 256
          )
        }

        if (systemName != "Java")
          assert(serialization.findSerializerFor(orig).getClass == classOf[KryoSerializer])
        else
          assert(serialization.findSerializerFor(orig).getClass != classOf[KryoSerializer])

        val serialized = serialization.serialize(orig)
        assert(serialized.isSuccess)

        val deserialized = serialization.deserialize(serialized.get, classOf[Array[Array[Int]]])
        assert(deserialized.isSuccess)

        val bytes = serialized.get
        println(s"Serialized to ${bytes.length} bytes")

        timeIt("Array Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Array Serialize:   ", iterations) {serialization.serialize(orig)}
        timeIt("Array Serialize:   ", iterations) {serialization.serialize(orig)}

        timeIt("Array Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[Array[Int]]]))
        timeIt("Array Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[Array[Int]]]))
        timeIt("Array Deserialize: ", iterations)(serialization.deserialize(bytes, classOf[Array[Array[Int]]]))
      }

    }

    testConfig("Zip", "akka-kryo-serialization.post-serialization-transformations = deflate")
    testConfig("LZ4", "akka-kryo-serialization.post-serialization-transformations = lz4")
    testConfig("AES", "akka-kryo-serialization.post-serialization-transformations = aes")
    testConfig("ZipAES",
      """
        |akka-kryo-serialization.post-serialization-transformations = "deflate,aes"
    """.stripMargin)
    testConfig("LZ4AES",
      """
        |akka-kryo-serialization.post-serialization-transformations = "lz4,aes"
    """.stripMargin)
    testConfig("Off", "")
    testConfig("Unsafe", "akka-kryo-serialization.use-unsafe = true")
    testConfig("UnsafeLZ4",
      """
        |akka-kryo-serialization.use-unsafe = true
        |akka-kryo-serialization.post-serialization-transformations = lz4
    """.stripMargin)
    testConfig("Java",
      """akka.actor.serialization-bindings {
        |"scala.Product" = java
        |"akka.actor.ActorRef" = java
        |"scala.collection.immutable.TreeMap" = java
        |"[Lscala.collection.immutable.TreeMap;" = java
        |"scala.collection.mutable.HashMap" = java
        |"[Lscala.collection.mutable.HashMap;" = java
        |"scala.collection.immutable.HashMap" = java
        |"[Lscala.collection.immutable.HashMap;" = java
        |"scala.collection.mutable.AnyRefMap" = java
        |"[Lscala.collection.mutable.AnyRefMap;" = java
        |"scala.collection.immutable.LongMap" = java
        |"[Lscala.collection.immutable.LongMap;" = java
        |"scala.collection.mutable.LongMap" = java
        |"[Lscala.collection.mutable.LongMap;" = java
        |"scala.collection.immutable.HashSet" = java
        |"[Lscala.collection.immutable.HashSet;" = java
        |"scala.collection.immutable.TreeSet" = java
        |"[Lscala.collection.immutable.TreeSet;" = java
        |"scala.collection.immutable.BitSet" = java
        |"[Lscala.collection.immutable.BitSet;" = java
        |"scala.collection.mutable.HashSet" = java
        |"[Lscala.collection.mutable.HashSet;" = java
        |"scala.collection.mutable.TreeSet" = java
        |"[Lscala.collection.mutable.TreeSet;" = java
        |"scala.collection.mutable.BitSet" = java
        |"[Lscala.collection.mutable.BitSet;" = java
        |"scala.collection.immutable.Vector" = java
        |"[Lscala.collection.immutable.Vector;" = java
        |"[[I" = java
        |}
        |""".stripMargin)
  }
}