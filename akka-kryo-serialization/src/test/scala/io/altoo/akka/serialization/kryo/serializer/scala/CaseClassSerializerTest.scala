package io.altoo.akka.serialization.kryo.serializer.scala

import akka.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.KryoSerializer
import io.altoo.akka.serialization.kryo.testkit.AbstractAkkaTest
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.collection.parallel.CollectionConverters._
import scala.util.Random

case class ScalaCaseClassSample(name: String, value: Double)
object CaseClassSerializerTest {

  private val config =
    """
      |akka {
      |  actor {
      |    serializers {
      |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "io.altoo.akka.serialization.kryo.serializer.scala.ScalaCaseClassSample" = kryo
      |    }
      |  }
      |}
      |akka-kryo-serialization {
      |  #trace = true
      |  #id-strategy = "default"
      |  #implicit-registration-logging = true
      |  #post-serialization-transformations = off
      |}
      |""".stripMargin
}

class CaseClassSerializerTest extends AbstractAkkaTest(ConfigFactory.parseString(CaseClassSerializerTest.config)) with AnyFlatSpecLike with Matchers {
  private val serialization = SerializationExtension(system)

  behavior of "case class serialization"

  it should "serialize/deserialize single case class" in {
    // serialize
    val value = ScalaCaseClassSample("foo", 123)
    val serializer = serialization.findSerializerFor(value)
    serializer shouldBe a[KryoSerializer]

    val serialized = serialization.serialize(value)
    serialized.isSuccess shouldBe true

    // deserialize
    val deserialized = serialization.deserialize(serialized.get, classOf[ScalaCaseClassSample])
    deserialized.isSuccess shouldBe true
    deserialized.get shouldBe value
  }

  it should "serialize/deserialize lots of case classes" in {
    val rnd = new Random()
    val values = (1 to 1000000).map(_ => ScalaCaseClassSample(rnd.nextString(rnd.nextInt(1000)), rnd.nextDouble()))
    val deserializedValues = values.map { value =>
      val serialized = serialization.serialize(value)
      serialized.isSuccess shouldBe true
      val deserialized = serialization.deserialize(serialized.get, classOf[ScalaCaseClassSample])
      deserialized.isSuccess shouldBe true
      deserialized.get
    }
    values.zip(deserializedValues).foreach{case (v,dv) => dv shouldBe v}
  }

  it should "serialize/deserialize lots of case classes in parallel" in {
    val rnd = new Random()
    val values = (1 to 1000000).map(_ => ScalaCaseClassSample(rnd.nextString(rnd.nextInt(1000)), rnd.nextDouble()))
    val deserializedValues = values.par.map { value =>
      val serialized = serialization.serialize(value)
      serialized.isSuccess shouldBe true
      val deserialized = serialization.deserialize(serialized.get, classOf[ScalaCaseClassSample])
      deserialized.isSuccess shouldBe true
      deserialized.get
    }
    values.zip(deserializedValues).foreach{case (v,dv) => dv shouldBe v}
  }
}





