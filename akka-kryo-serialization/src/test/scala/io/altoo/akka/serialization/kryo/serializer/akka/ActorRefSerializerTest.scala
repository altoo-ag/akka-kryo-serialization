package io.altoo.akka.serialization.kryo.serializer.akka

import akka.actor.{Actor, ActorRef, Props}
import akka.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.KryoSerializer
import io.altoo.akka.serialization.kryo.testkit.AbstractAkkaTest

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

class ActorRefSerializerTest extends AbstractAkkaTest(ConfigFactory.parseString(ActorRefSerializerTest.testConfig)) {
  private val serialization = SerializationExtension(system)


  behavior of "ActorRefSerializer"

  it should "serialize and deserialize actorRef" in {
    val value: ActorRef = system.actorOf(Props(new Actor {def receive: Receive = PartialFunction.empty}))

    // serialize
    val serializer = serialization.findSerializerFor(value)
    serializer shouldBe a[KryoSerializer]

    val serialized = serialization.serialize(value)
    serialized shouldBe a[util.Success[_]]

    // deserialize
    val deserialized = serialization.deserialize(serialized.get, classOf[ActorRef])
    deserialized shouldBe util.Success(value)
  }
}
