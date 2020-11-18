package io.altoo.akka.serialization.kryo.serializer.akka

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.serialization.SerializationExtension
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.KryoSerializer
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

object ActorRefSerializerTest {
  private val testConfig =
    """
      |akka {
      |  actor {
      |    serializers {
      |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "akka.actor.ActorRef" = kryo
      |    }
      |  }
      |}
      |akka-kryo-serialization {
      |  trace = true
      |  id-strategy = "default"
      |  implicit-registration-logging = true
      |  post-serialization-transformations = off
      |}
      |""".stripMargin
}

class ActorRefSerializerTest extends TestKit(ActorSystem("testSystem", ConfigFactory.parseString(ActorRefSerializerTest.testConfig))) with AnyFlatSpecLike with Matchers {
  private val serialization = SerializationExtension(system)


  behavior of "ActorRefSerializer"

  it should "serialize and deserialize actorRef" in {
    val value: ActorRef = system.actorOf(Props(new Actor {def receive: Receive = PartialFunction.empty}))

    // serialize
    val serializer = serialization.findSerializerFor(value)
    serializer shouldBe a[KryoSerializer]

    val serialized = serialization.serialize(value)
    serialized.isSuccess shouldBe true

    // deserialize
    val deserialized = serialization.deserialize(serialized.get, classOf[ActorRef])
    deserialized.isSuccess shouldBe true
    deserialized.get shouldBe value
  }
}
