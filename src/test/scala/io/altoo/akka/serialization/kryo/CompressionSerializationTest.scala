package io.altoo.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAllConfigMap
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.immutable.{HashMap, TreeMap}
import scala.collection.mutable.AnyRefMap

object CompressionSerializationTest {
  private val defaultConfig =
    """
      |akka {
      |  actor {
      |   allow-java-serialization = off
      |   serializers {
      |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "java.io.Serializable" = none
      |
      |      "scala.collection.immutable.TreeMap" = kryo
      |      "[Lscala.collection.immutable.TreeMap;" = kryo
      |      "scala.collection.immutable.HashMap" = kryo
      |      "[Lscala.collection.immutable.HashMap;" = kryo
      |      "scala.collection.immutable.HashSet" = kryo
      |      "[Lscala.collection.immutable.HashSet;" = kryo
      |      "scala.collection.immutable.TreeSet" = kryo
      |      "[Lscala.collection.immutable.TreeSet;" = kryo
      |      "scala.collection.mutable.HashMap" = kryo
      |      "[Lscala.collection.mutable.HashMap;" = kryo
      |      "scala.collection.mutable.AnyRefMap" = kryo
      |      "[Lscala.collection.mutable.AnyRefMap;" = kryo
      |      "scala.collection.mutable.HashSet" = kryo
      |      "[Lscala.collection.mutable.HashSet;" = kryo
      |      "scala.collection.mutable.TreeSet" = kryo
      |      "[Lscala.collection.mutable.TreeSet;" = kryo
      |    }
      |  }
      |}
      |kryo-serialization {
      |  type = "nograph"
      |  id-strategy = "incremental"
      |  kryo-reference-map = false
      |  buffer-size = 65536
      |  post-serialization-transformations = off
      |  implicit-registration-logging = true
      |}
      |""".stripMargin
}

class CompressionSerializationTest extends AnyFlatSpec with BeforeAndAfterAllConfigMap {

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
  testConfig("Unsafe", "akka.actor.kryo.use-unsafe = true")
  testConfig("UnsafeLZ4",
    """akka.actor.kryo.use-unsafe = true
      |akka.actor.kryo.post-serialization-transformations = lz4
    """.stripMargin)

  def testConfig(systemName: String, config: String): Unit = {
    val system = ActorSystem(systemName, ConfigFactory.parseString(config).withFallback(ConfigFactory.parseString(CompressionSerializationTest.defaultConfig)))
    val serialization = SerializationExtension(system)

    s"$systemName KryoSerializer" should "serialize and deserialize immutable TreeMap[String,Any] successfully" in {
      val tm = TreeMap[String, Any](
        "foo" -> 123.3,
        "bar" -> "something as a text",
        "baz" -> null,
        "boom" -> true,
        "hash" -> HashMap[Int, Int](1 -> 200, 2 -> 300, 500 -> 3))

      if (systemName != "Java")
        assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
      else
        assert(serialization.findSerializerFor(tm).getClass != classOf[KryoSerializer])

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

      if (systemName != "Java")
        assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
      else
        assert(serialization.findSerializerFor(tm).getClass != classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[HashMap[String, Any]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize mutable AnyRefMap[String,Any] successfully" in {
      if (systemName != "Java") {
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
    }

    it should "serialize and deserialize mutable HashMap[String,Any] successfully" in {
      val tm = scala.collection.mutable.HashMap[String, Any](
        "foo" -> 123.3,
        "bar" -> "something as a text",
        "baz" -> null,
        "boom" -> true,
        "hash" -> HashMap[Int, Int](1 -> 200, 2 -> 300, 500 -> 3))

      if (systemName != "Java")
        assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
      else
        assert(serialization.findSerializerFor(tm).getClass != classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.mutable.HashMap[String, Any]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    // Sets
    it should "serialize and deserialize immutable HashSet[String] successfully" in {
      val tm = scala.collection.immutable.HashSet[String]("foo", "bar", "baz", "boom")

      if (systemName != "Java")
        assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
      else
        assert(serialization.findSerializerFor(tm).getClass != classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.immutable.HashSet[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize immutable TreeSet[String] successfully" in {
      val tm = scala.collection.immutable.TreeSet[String]("foo", "bar", "baz", "boom")

      if (systemName != "Java")
        assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
      else
        assert(serialization.findSerializerFor(tm).getClass != classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.immutable.TreeSet[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize mutable HashSet[String] successfully" in {
      val tm = scala.collection.mutable.HashSet[String]("foo", "bar", "baz", "boom")

      if (systemName != "Java")
        assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
      else
        assert(serialization.findSerializerFor(tm).getClass != classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.mutable.HashSet[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }

    it should "serialize and deserialize mutable TreeSet[String] successfully" in {
      val tm = scala.collection.mutable.TreeSet[String]("foo", "bar", "baz", "boom")

      if (systemName != "Java")
        assert(serialization.findSerializerFor(tm).getClass == classOf[KryoSerializer])
      else
        assert(serialization.findSerializerFor(tm).getClass != classOf[KryoSerializer])

      val serialized = serialization.serialize(tm)
      assert(serialized.isSuccess)

      val deserialized = serialization.deserialize(serialized.get, classOf[scala.collection.mutable.TreeSet[String]])
      assert(deserialized.isSuccess)
      assert(deserialized.get == tm)
    }
  }
}
