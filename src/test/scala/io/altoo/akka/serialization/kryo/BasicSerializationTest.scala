package io.altoo.akka.serialization.kryo

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

object BasicSerializationTest {

  private val config =
    s"""
       |akka {
       |  loggers = ["akka.event.Logging$$DefaultLogger"]
       |  loglevel = "WARNING"
       |
       |  actor {
       |    serializers {
       |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
       |    }
       |
       |    serialization-bindings {
       |      "scala.Product" = kryo
       |    }
       |  }
       |}
       |
       |akka-kryo-serialization {
       |  trace = true
       |  id-strategy = "incremental"
       |  implicit-registration-logging = true
       |  post-serialization-transformations = off
       |}
       |""".stripMargin
}

class BasicSerializationTest extends TestKit(ActorSystem("example", ConfigFactory.parseString(BasicSerializationTest.config))) with AnyFlatSpecLike with Matchers with Inside {
  private val serialization = SerializationExtension(system)

  private val testList = List(1 to 40: _*)

  behavior of "KryoSerializer"

  it should "be selected for lists" in {
    // Find the Serializer for it
    val serializer = serialization.findSerializerFor(testList)
    serializer.getClass.equals(classOf[KryoSerializer]) shouldBe true

    // Check serialization/deserialization
    val serialized = serialization.serialize(testList)
    serialized shouldBe a[util.Success[_]]

    val deserialized = serialization.deserialize(serialized.get, testList.getClass)
    inside(deserialized) {
      case util.Success(v) => v shouldBe testList
    }
  }
}
