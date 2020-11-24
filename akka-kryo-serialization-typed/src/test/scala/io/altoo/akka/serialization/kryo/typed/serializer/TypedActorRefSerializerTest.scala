package io.altoo.akka.serialization.kryo.typed.serializer

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import io.altoo.akka.serialization.kryo.KryoSerializer
import io.altoo.akka.serialization.kryo.typed.testkit.AbstractTypedAkkaTest

object TypedActorRefSerializerTest {
  private val testConfig =
    """
      |akka {
      |  actor {
      |    serializers {
      |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "akka.actor.typed.ActorRef" = kryo
      |      "akka.actor.typed.internal.adapter.ActorRefAdapter" = kryo
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

  private trait Msg
}

class TypedActorRefSerializerTest extends AbstractTypedAkkaTest(ConfigFactory.parseString(TypedActorRefSerializerTest.testConfig)) {
  import TypedActorRefSerializerTest._

  private val serialization = SerializationExtension(testKit.system.classicSystem)

  behavior of "TypedActorRefSerializer"

  it should "serialize and deserialize actorRef" in {
    val value: ActorRef[Msg] = testKit.spawn(Behaviors.ignore[Msg])

    // serialize
    val serializer = serialization.findSerializerFor(value)
    serializer shouldBe a[KryoSerializer]

    val serialized = serialization.serialize(value)
    serialized shouldBe a[util.Success[_]]

    // deserialize
    val deserialized = serialization.deserialize(serialized.get, classOf[ActorRef[Msg]])
    deserialized shouldBe util.Success(value)
  }
}
